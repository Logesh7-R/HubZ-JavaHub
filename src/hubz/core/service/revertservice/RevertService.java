package hubz.core.service.revertservice;

import hubz.context.HubzContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.service.RepositoryHelper;
import hubz.io.FileManager;
import hubz.io.JsonSerializer;
import hubz.model.RevertResult;
import hubz.model.RevertResult.Status;
import hubz.model.commitmodel.CommitModel;
import hubz.model.indexmodel.IndexModel;
import hubz.model.treemodel.TreeEntry;
import hubz.model.treemodel.TreeModel;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RevertService {

    public RevertResult revert(String targetCommitHash) throws IOException, RepositoryNotFoundException {
        // Validate repo exists
        File rootDir = HubzContext.getRootDir();
        if (rootDir == null || !rootDir.exists()) {
            throw new RepositoryNotFoundException("Repository root not found: " + (rootDir == null ? "null" : rootDir.getAbsolutePath()));
        }

        // Traversal of graph to verify commit belongs to history
        RepositoryHelper helper = new RepositoryHelper();
        List<String> history = helper.traverseGraphFromHead();
        if (history == null || history.isEmpty() || !history.contains(targetCommitHash)) {
            return new RevertResult(Status.NOT_FOUND, targetCommitHash, null, null,
                    "Commit not found in current branch history: " + targetCommitHash);
        }

        // Load current index (last committed snapshot)
        IndexModel oldIndex = helper.loadIndex();
        if (oldIndex == null) oldIndex = new IndexModel();

        // Scan working directory into newIndex
        IndexModel newIndex = new IndexModel();
        helper.scanWorkingDirectory(rootDir, newIndex);

        // If working directory is dirty (has uncommitted changes), abort
        Map<String, String> created = new LinkedHashMap<>();
        Map<String, String> modified = new LinkedHashMap<>();
        Map<String, String> deleted = new LinkedHashMap<>();

        helper.calculateWorkingDirectoryDiff(oldIndex, newIndex, created, modified, deleted);

        if (!(created.isEmpty() && modified.isEmpty() && deleted.isEmpty())) {
            return new RevertResult(Status.ABORTED, targetCommitHash, null, null,
                    "Working directory has uncommitted changes. Please commit before revert.");
        }

        // Load target commit and its tree
        File commitFile = new File(rootDir, HubzPath.COMMITS_DIR +File.separator+ targetCommitHash + ".json");
        if (!commitFile.exists()) {
            return new RevertResult(Status.NOT_FOUND, targetCommitHash, null, null,
                    "Commit object missing on disk: " + commitFile.getAbsolutePath());
        }
        CommitModel targetCommit = JsonSerializer.readJsonFile(commitFile, CommitModel.class);
        if (targetCommit == null) {
            return new RevertResult(Status.NOT_FOUND, targetCommitHash, null, null,
                    "Failed to read commit object: " + commitFile.getAbsolutePath());
        }
        targetCommit.setHash(targetCommitHash);

        String treeHash = targetCommit.getTreeHash();
        File treeFile = new File(rootDir, HubzPath.TREES_DIR +File.separator+ treeHash + ".json");
        if (!treeFile.exists()) {
            return new RevertResult(Status.NOT_FOUND, targetCommitHash, null, null,
                    "Tree object missing for commit: " + treeHash);
        }
        TreeModel treeModel = JsonSerializer.readJsonFile(treeFile, TreeModel.class);
        if (treeModel == null) {
            return new RevertResult(Status.NOT_FOUND, targetCommitHash, null, null,
                    "Failed to read tree object: " + treeFile.getAbsolutePath());
        }
        treeModel.setHash(treeHash);

        Map<String, TreeEntry> files = treeModel.getFiles();
        Map<String,String> changedFiles = new LinkedHashMap<>();
        Map<String,String> conflictFiles = new LinkedHashMap<>();

        //Reverting actions
        //Created -> delete it, if there is no conflict(when target commit' newBlob != Current Blob)
        //Modified -> Returned to old blob content, if no conflict (when target commit's newBlob != current blob)
        //Deleted -> Created it, if no conflict(when file exist in same file name)
        for (Map.Entry<String, TreeEntry> e : files.entrySet()) {
            String relativePath = e.getKey();
            TreeEntry entry = e.getValue();

            // Get current working file and its corresponding hash
            File workingFile = new File(rootDir, relativePath);
            boolean workingExists = workingFile.exists();
            boolean indexHas = newIndex.getFiles().containsKey(relativePath);
            String indexHash = indexHas ? newIndex.getFiles().get(relativePath).getHash() : null;

            // case 1 : if created in target commit
            if (entry.isCreated()) {
                // If new blob in target commit is same as index blob, then there is no conflict
                //Safe to delete it
                String createdBlob = entry.getCreatedBlob();
                if (indexHas && indexHash != null && indexHash.equals(createdBlob)) {
                    // safe to delete working file
                    if (workingExists) {
                        FileManager.deleteFileAndCleanParents(workingFile);
                        changedFiles.put(relativePath,"DELETED");
                    }
                } else {
                    // conflict
                    if (indexHas && indexHash != null && !indexHash.equals(createdBlob)) {
                        conflictFiles.put(relativePath, "DELETED");
                        helper.handleConflict(relativePath, indexHash, createdBlob, targetCommit);
                    }
                }
            }

            // case 2 : if file was deleted in target commit
            else if (entry.isDeleted()) {
                String deletedBlob = entry.getDeletedBlob();
                // While restoring deleted, check if any file exists in same name, if no then create that file
                if (!workingExists) {
                    if (deletedBlob != null) {
                        File blobFile = new File(rootDir, HubzPath.BLOBS_DIR +File.separator+ deletedBlob + ".txt");
                        if (blobFile.exists()) {
                            String content = FileManager.readFile(blobFile.getAbsolutePath());
                            FileManager.createDir(workingFile.getParent());
                            FileManager.createFile(workingFile.getAbsolutePath(), content);
                            changedFiles.put(relativePath,"CREATED");
                        } else {
                            // blob missing -> conflict
                            conflictFiles.put(relativePath,"CREATED");
                            helper.handleConflict(relativePath, indexHash, deletedBlob, targetCommit);
                        }
                    } else {
                        // no blob info -> conflict
                        conflictFiles.put(relativePath,"CREATED");
                        helper.handleConflict(relativePath, indexHash, deletedBlob, targetCommit);
                    }
                } else {
                    // working file exists. Two possibilities
                    //1. Target file exists and unchanged -> no conflict
                    //2. Target file exists and changed -> conflict
                    if (indexHas && indexHash != null && indexHash.equals(entry.getModifiedBlob())) {
                        // File was deleted in target commit, but exists now and unchanged
                        if (deletedBlob != null) {
                            File blobFile = new File(rootDir, HubzPath.BLOBS_DIR +File.separator+
                                    deletedBlob + ".txt");
                            if (blobFile.exists()) {
                                String content = FileManager.readFile(blobFile.getAbsolutePath());
                                FileManager.atomicWrite(workingFile, content);
                                changedFiles.put(relativePath,"CREATED");
                            } else {
                                // blob missing -> conflict
                                conflictFiles.put(relativePath,"CREATED");
                                helper.handleConflict(relativePath, indexHash, deletedBlob, targetCommit);
                            }
                        } else {
                            // no blob info -> conflict
                            conflictFiles.put(relativePath,"CREATED");
                            helper.handleConflict(relativePath, indexHash, deletedBlob, targetCommit);
                        }
                    } else {
                        // user changed file -> conflict
                        conflictFiles.put(relativePath,"CREATED");
                        helper.handleConflict(relativePath, indexHash, entry.getDeletedBlob(), targetCommit);
                    }
                }
            }

            // case 3:if file was modified in target commit
            else if (entry.isModified()) {
                String targetOldBlob = entry.getOldBlob();      // blob in target (older)
                String targetNewBlob = entry.getModifiedBlob(); // blob of "modified" blob
                // modified blob equals with current blob hash, then no conflict, restore to old blob
                if (indexHas && indexHash != null && indexHash.equals(targetNewBlob)) {
                    // safe to revert file to old blob
                    File srcBlob = new File(rootDir, HubzPath.BLOBS_DIR +File.separator+ targetOldBlob + ".txt");
                    if (srcBlob.exists()) {
                        String content = FileManager.readFile(srcBlob.getAbsolutePath());
                        FileManager.atomicWrite(workingFile, content);
                        changedFiles.put(relativePath,"MODIFIED");
                    } else {
                        // blob missing -> conflict
                        conflictFiles.put(relativePath,"MODIFIED");
                        helper.handleConflict(relativePath, indexHash, targetOldBlob, targetCommit);
                    }
                } else {
                    // working copy differs from index marker -> conflict
                    conflictFiles.put(relativePath,"MODIFIED");
                    helper.handleConflict(relativePath, indexHash, targetNewBlob, targetCommit);
                }
            }
            else {
                // nothing for this file
            }
        }

        // Determine final status
        Status status = conflictFiles.isEmpty() ? Status.SUCCESS : Status.CONFLICT;
        String message = conflictFiles.isEmpty()
                ? "Revert prepared successfully. Please review changes and commit."
                : "Revert prepared with conflicts. Resolve conflicts and commit.";

        return new RevertResult(status, targetCommitHash, changedFiles, conflictFiles, message);
    }
}
