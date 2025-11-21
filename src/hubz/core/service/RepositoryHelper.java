package hubz.core.service;

import com.google.gson.reflect.TypeToken;
import hubz.context.HubzContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.io.FileManager;
import hubz.io.JsonSerializer;
import hubz.model.clustermodel.ClusterModel;
import hubz.model.clustermodel.SnapshotInfo;
import hubz.model.commitmodel.CommitModel;
import hubz.model.indexmodel.IndexEntry;
import hubz.model.indexmodel.IndexModel;
import hubz.model.metamodel.MetaModel;
import hubz.model.resetmodel.ResetStackModel;
import hubz.model.treemodel.TreeEntry;
import hubz.model.treemodel.TreeModel;
import hubz.util.HashUtil;
import hubz.util.HubzPath;
import hubz.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;

public class RepositoryHelper {
    public void scanWorkingDirectory(File currentDir, IndexModel index) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        File rootDir = HubzContext.getRootDir();
        File hubzDir = new File(rootDir, HubzPath.HUBZ_DIR);

        for (File file : files) {
            if (file.getAbsolutePath().startsWith(hubzDir.getAbsolutePath()))
                continue;

            if (file.isDirectory()) {
                scanWorkingDirectory(file, index);
            } else {
                String relativePath = rootDir.toPath().relativize(file.toPath()).toString();
                IndexEntry entry = new IndexEntry(
                        "",
                        file.length(),
                        file.lastModified());
                index.getFiles().put(relativePath, entry);
            }
        }
    }

    //Detect files which are modified, created and deleted
    //NewIndex will store unmodified files and its blob hash
    public void calculateWorkingDirectoryDiff(IndexModel oldIndex, IndexModel newIndex,
                                              Map<String, String> created, Map<String, String> modified,
                                              Map<String, String> deleted) throws IOException {

        //Finding Modified or Created files
        for (Map.Entry<String, IndexEntry> entry : newIndex.getFiles().entrySet()) {
            String path = entry.getKey();
            IndexEntry newFile = entry.getValue();
            IndexEntry oldFile = oldIndex.getFiles().get(path);

            if (oldFile == null) {
                created.put(path, new File(HubzContext.getRootDir(), path).getAbsolutePath());
            } else {
                boolean metaChanged =
                        oldFile.getSize() != newFile.getSize() ||
                                oldFile.getMtime() != newFile.getMtime();

                if (metaChanged) {
                    // Compute HASH for new working file
                    String workingFilePath = new File(HubzContext.getRootDir(), path).getAbsolutePath();
                    String newHash = HashUtil.sha256File(workingFilePath);

                    // If hash differs → file truly modified
                    if (!newHash.equals(oldFile.getHash())) {
                        modified.put(path, workingFilePath);
                    } else {
                        // Meta changed but content same → treat as unchanged
                        IndexEntry newEntry = newIndex.getFiles().get(path);
                        newEntry.setHash(oldFile.getHash());
                    }
                } else {
                    // size and mTime same → unchanged
                    IndexEntry newEntry = newIndex.getFiles().get(path);
                    newEntry.setHash(oldFile.getHash());
                }
            }
        }

        //Finding Deleted Files
        for (Map.Entry<String, IndexEntry> entry : oldIndex.getFiles().entrySet()) {
            String path = entry.getKey();
            if (!newIndex.getFiles().containsKey(path)) {
                deleted.put(path, new File(HubzContext.getRootDir(), path).getAbsolutePath());
            }
        }
    }

    //Getting current working branch name
    public String getCurrentBranchName() throws RepositoryNotFoundException, IOException {
        File headFile = new File(HubzContext.getRootDir(), HubzPath.HEAD_FILE);
        String headContent = FileManager.readFile(headFile.getAbsolutePath()).trim();
        String[] currentBranch = null;
        if (headContent.startsWith("ref:")) {
            String regex = Pattern.quote(File.separator);
            currentBranch = headContent.split(regex);
        } else {
            throw new RepositoryNotFoundException("Invalid HEAD reference. Please check repository state. " +
                    "(ServiceHelper -> getBranch())");
        }
        return currentBranch[currentBranch.length - 1];
    }

    //Getting current working branch path
    public String getBranchPath() throws RepositoryNotFoundException, IOException {
        File headFile = new File(HubzContext.getRootDir(), HubzPath.HEAD_FILE);
        String headContent = FileManager.readFile(headFile.getAbsolutePath()).trim();
        String branchPath = null;
        if (headContent.startsWith("ref:")) {
            branchPath = headContent.substring(4).trim();
        } else {
            throw new RepositoryNotFoundException("Invalid HEAD reference. Please check repository state. " +
                    "(ServiceHelper -> getBranch())");
        }
        return branchPath;
    }

    //Now getting parent commit or recent commit
    public String getHeadCommitHash() throws RepositoryNotFoundException, IOException {
        File hubzDir = new File(HubzContext.getRootDir(), HubzPath.HUBZ_DIR);
        File branchRefFile = new File(hubzDir, getBranchPath());
        String parentHash = null;
        if (FileManager.exists(branchRefFile.getAbsolutePath())) {
            parentHash = FileManager.readFile(branchRefFile.getAbsolutePath()).trim();
            if (parentHash.isEmpty()) parentHash = null;
        }
        return parentHash;
    }

    //Traversing graph from head
    public List<String> traverseGraphFromHead() throws IOException, RepositoryNotFoundException {

        Map<String, List<String>> graph = loadCommitGraph();
        if (graph == null) return Collections.emptyList();

        String head = getHeadCommitHash();

        List<String> order = new ArrayList<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(head);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current == null) continue;

            order.add(current);

            List<String> children = graph.get(current);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return order;
    }

    //Load meta file from hubzDirectory
    public MetaModel loadMeta() throws IOException {
        File metaFile = new File(HubzContext.getRootDir(), HubzPath.META_FILE);
        MetaModel meta;

        if (!metaFile.exists()) {
            meta = new MetaModel();
            meta.setAuthors(HubzContext.getAllAuthorsAsList());
        } else {
            meta = JsonSerializer.readJsonFile(metaFile, MetaModel.class);
            if (meta == null) meta = new MetaModel();


            if (!HubzContext.getAllAuthorsAsList().isEmpty()) {
                meta.setAuthors(HubzContext.getAllAuthorsAsList());
            } else if (meta.getAuthors() != null && !meta.getAuthors().isEmpty()) {
                HubzContext.setAllAuthorsFromList(meta.getAuthors());
            }
        }
        return meta;
    }

    //Load index file from hubzDirectory
    public IndexModel loadIndex() throws IOException {
        File indexFile = new File(HubzContext.getRootDir(), HubzPath.INDEX_FILE);
        IndexModel index;

        if (!indexFile.exists()) {
            index = new IndexModel();
        } else {
            index = JsonSerializer.readJsonFile(indexFile, IndexModel.class);
            if (index == null) index = new IndexModel();
        }
        return index;
    }

    //Conflict manager, this will handle merge, revert or reset conflict
    public void handleConflict(String filePath,
                               String currentBlobHash,
                               String oldBlobHash,
                               CommitModel targetCommit) throws IOException {
        if (filePath == null) throw new IllegalArgumentException("filePath cannot be null");

        File repoRoot = HubzContext.getRootDir();
        File targetFile = new File(filePath);
        if (!targetFile.isAbsolute()) {
            targetFile = new File(repoRoot, filePath);
        }

        StringBuilder builder = new StringBuilder();

        // HEAD marker and current file content (if available)
        builder.append("<<<<<<< HEAD").append(System.lineSeparator());
        if (currentBlobHash != null && !currentBlobHash.isEmpty()) {
            File currentBlobFile = new File(repoRoot, HubzPath.BLOBS_DIR + File.separator + currentBlobHash + ".txt");
            if (currentBlobFile.exists()) {
                builder.append(FileManager.readFile(currentBlobFile.getAbsolutePath()));
            } else if (targetFile.exists()) {
                // read the working file itself
                builder.append(FileManager.readFile(targetFile.getAbsolutePath()));
            } else {
                builder.append("<<missing current content>>");
            }
        } else {
            if (targetFile.exists()) {
                builder.append(FileManager.readFile(targetFile.getAbsolutePath()));
            } else {
                builder.append("<<missing current content>>");
            }
        }
        builder.append(System.lineSeparator());

        // divider
        builder.append("=======").append(System.lineSeparator());

        // (old commit) content
        if (oldBlobHash != null && !oldBlobHash.isEmpty()) {
            File oldBlobFile = new File(repoRoot, HubzPath.BLOBS_DIR + File.separator + oldBlobHash + ".txt");
            if (oldBlobFile.exists()) {
                builder.append(FileManager.readFile(oldBlobFile.getAbsolutePath()));
            } else {
                builder.append("<<missing target commit content for blob ").append(oldBlobHash).append(">>");
            }
        } else {
            builder.append("<<missing target commit content>>");
        }
        builder.append(System.lineSeparator());

        // footer with commit info
        if (targetCommit != null) {
            builder.append(">>>>>>> Commit ")
                    .append(targetCommit.getHash() != null ? targetCommit.getHash() : "<unknown>")
                    .append(" (").append(targetCommit.getMessage() != null ? targetCommit.getMessage() : "").append(")")
                    .append(System.lineSeparator());
        } else {
            builder.append(">>>>>>> Commit <unknown>").append(System.lineSeparator());
        }

        // ensure parent dir exists
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create parent directories for conflict file: " + parent.getAbsolutePath());
            }
        }

        // write conflict file atomically
        FileManager.atomicWrite(targetFile, builder.toString());
    }

    // This helper is used to rebuild target index model
    public IndexModel rebuildIndexAtCommit(String targetCommitHash)
            throws IOException, RepositoryNotFoundException {

        // Validate commit hash
        List<String> branchHistory = traverseGraphFromHead();

        boolean commitInHistory = branchHistory != null && branchHistory.contains(targetCommitHash);
        boolean allowedBecauseUndo = false;

        //If commit hash not present in history, then check if it is present in reset stack
        if (!commitInHistory) {

            ResetStackModel stack = JsonSerializer.readJsonFile(
                    HubzPath.getResetStackFilePath(), ResetStackModel.class
            );

            if (stack != null && stack.getResetStack() != null) {
                for (List<String> record : stack.getResetStack()) {
                    String storedCommit = record.getFirst();
                    if (storedCommit.equals(targetCommitHash)) {
                        allowedBecauseUndo = true;
                        break;
                    }
                }
            }

            if (!allowedBecauseUndo) {
                return null;
            }
        }

        // Find nearest snapshot
        SnapshotInfo snapshot = findClosestSnapshot(targetCommitHash);
        if (snapshot == null) {
            throw new IOException("No snapshot available to rebuild index for commit: " + targetCommitHash);
        }

        // Load Snapshot Index
        File snapFile = HubzPath.getSnapshotFilePath(snapshot.getPath());
        IndexModel rebuiltIndex = JsonSerializer.readJsonFile(snapFile, IndexModel.class);

        if (rebuiltIndex == null) {
            rebuiltIndex = new IndexModel();
        }

        //Load files and its meta detail present in index model
        Map<String, IndexEntry> fileMap = rebuiltIndex.getFiles();
        if (fileMap == null) {
            fileMap = new LinkedHashMap<>();
        }

        // Get path of commit hash to rebuild index
        // Snapshot index's commit -> Target commit
        List<String> path = computeCommitPath(targetCommitHash, snapshot.getCommitHash());
        if (path == null || path.isEmpty()) {
            rebuiltIndex.setFiles(fileMap);
            return rebuiltIndex;
        }

        // Update index based on commit changes done by commits in the path
        for (String commitHash : path) {
            CommitModel commit = JsonSerializer.readJsonFile(
                    HubzPath.getCommitFilePath(commitHash), CommitModel.class
            );

            if (commit == null) continue;

            TreeModel tree = JsonSerializer.readJsonFile(
                    HubzPath.getTreeFilePath(commit.getTreeHash()), TreeModel.class
            );
            if (tree == null || tree.getFiles() == null) continue;

            // Iterate every file in each commit and update changes in index
            for (Map.Entry<String, TreeEntry> e : tree.getFiles().entrySet()) {
                String filePath = e.getKey();
                TreeEntry treeEntry = e.getValue();

                if (treeEntry.isCreated() || treeEntry.isModified()) {
                    //Update IndexEntry on every file if it is edited or deleted
                    IndexEntry idx = new IndexEntry();
                    idx.setHash(treeEntry.getNewBlob());
                    idx.setSize(treeEntry.getSize());
                    idx.setMtime(treeEntry.getMtime());
                    fileMap.put(filePath, idx);
                } else if (treeEntry.isDeleted()) {
                    // Delete file in index
                    fileMap.remove(filePath);
                }
            }
        }

        rebuiltIndex.setFiles(fileMap);
        return rebuiltIndex;
    }

    public Map<String, List<String>> loadCommitGraph() throws IOException {
        //Load commit graph from graph file
        File graphFile = new File(HubzContext.getRootDir(), HubzPath.GRAPH_FILE);
        String json = FileManager.readFile(graphFile.getAbsolutePath());
        Type type = new TypeToken<LinkedHashMap<String, List<String>>>() {
        }.getType();
        return JsonUtil.fromJson(json, type);
    }

    //Finding nearest snapshot, only parent snapshot is valid
    public SnapshotInfo findClosestSnapshot(String targetCommitHash) throws IOException {
        Map<String, List<String>> graph = loadCommitGraph();
        if (graph == null) return null;

        // Traverse graph (BFS)
        Queue<String> queue = new ArrayDeque<>();
        queue.add(targetCommitHash);

        String snapshotHash = null;

        // BFS forward (Child -> Parent)
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (cur == null) continue;

            CommitModel cm = JsonSerializer.readJsonFile(HubzPath.getCommitFilePath(cur), CommitModel.class);
            int num = cm.getCommitNumber();

            // If commit number is 1 or divisible of 25 -> It is the nearest snapshot
            if (num == 1 || num % 25 == 0) {
                snapshotHash = cur;
                break;
            }

            List<String> children = graph.get(cur);
            if (children != null) queue.addAll(children);
        }

        if (snapshotHash == null) return null;

        ClusterModel cluster = JsonSerializer.readJsonFile(HubzPath.getClusterFilePath(), ClusterModel.class);
        for (SnapshotInfo si : cluster.getSnapshots()) {
            if (si.getCommitHash().equals(snapshotHash)) return si;
        }
        return null;
    }

    // Finding path from startCommit -> targetCommit
    public List<String> computeCommitPath(String startCommit, String targetCommit) throws IOException {
        Map<String, List<String>> graph = loadCommitGraph();
        if (graph == null) return Collections.emptyList();

        if (startCommit.equals(targetCommit)) return Collections.singletonList(startCommit);

        Queue<String> queue = new ArrayDeque<>();
        Map<String, String> parentMap = new HashMap<>();
        queue.add(startCommit);
        parentMap.put(startCommit, null);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            List<String> neighbors = graph.getOrDefault(cur, Collections.emptyList());

            // Load all parents, even terminated path can also be used
            for (String next : neighbors) {
                if (!parentMap.containsKey(next)) {
                    parentMap.put(next, cur);
                    queue.add(next);

                    if (next.equals(targetCommit)) {
                        return assembleCommitPath(parentMap, targetCommit);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    // Using map and target commit to build path for target commit from start commit
    private List<String> assembleCommitPath(Map<String, String> parentMap, String targetCommit) {
        List<String> path = new ArrayList<>();
        String node = targetCommit;

        // Will add only necessary path leads to target commit
        while (node != null) {
            path.add(node);
            node = parentMap.get(node);
        }
        return path;
    }

    // Rebuilding working folder structure based on given index model
    public void applyIndexToWorkingDirectory(IndexModel targetIndex) throws IOException {
        IndexModel current = new IndexModel();

        //Load current index.json file
        scanWorkingDirectory(HubzContext.getRootDir(), current);

        List<String> creating = new ArrayList<>();
        List<String> modifying = new ArrayList<>();
        List<String> deleting = new ArrayList<>();

        Map<String, IndexEntry> newFiles = targetIndex.getFiles();
        Map<String, IndexEntry> curFiles = current.getFiles();

        // Storing created and modified files
        for (String path : newFiles.keySet()) {
            if (!curFiles.containsKey(path)) {
                creating.add(path);
            } else if (!newFiles.get(path).getHash().equals(curFiles.get(path).getHash())) {
                modifying.add(path);
            }
        }

        // Storing deleted files
        for (String path : curFiles.keySet()) {
            if (!newFiles.containsKey(path)) {
                deleting.add(path);
            }
        }

        // Create files, if file didn't exist
        for (String path : creating) {
            String blobHash = newFiles.get(path).getHash();
            File blob = HubzPath.getBlobFilePath(blobHash);
            File working = new File(HubzContext.getRootDir(), path);

            // Creating file with content
            String content = FileManager.readFile(blob.getAbsolutePath());
            FileManager.createDir(working.getParent());
            FileManager.createFile(working.getAbsolutePath(), content);
        }

        // Modify files atomically
        for (String path : modifying) {
            String blobHash = newFiles.get(path).getHash();
            File blob = HubzPath.getBlobFilePath(blobHash);
            File working = new File(HubzContext.getRootDir(), path);

            String content = FileManager.readFile(blob.getAbsolutePath());
            FileManager.atomicWrite(working, content);
        }

        // Delete files
        for (String path : deleting) {
            File working = new File(HubzContext.getRootDir(), path);
            FileManager.deleteFileAndCleanParents(working);
        }
    }
}