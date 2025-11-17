package hubz.core.service.commitservice;

import hubz.context.HubzContext;
import hubz.core.exception.InvalidCommitMessageException;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.service.RepositoryHelper;
import hubz.io.JsonSerializer;
import hubz.model.OperationResult;
import hubz.model.clustermodel.ClusterModel;
import hubz.model.clustermodel.SnapshotInfo;
import hubz.model.commitmodel.CommitModel;
import hubz.model.indexmodel.IndexModel;
import hubz.model.metamodel.MetaModel;
import hubz.model.treemodel.TreeEntry;
import hubz.model.treemodel.TreeModel;
import hubz.io.FileManager;
import hubz.util.HubzPath;
import hubz.util.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommitService {
    public OperationResult commit(String message) throws InvalidCommitMessageException, RepositoryNotFoundException {
        //Check message is valid or not
        if (message == null || message.trim().isEmpty()) {
            throw new InvalidCommitMessageException("Commit message cannot be empty. (CommitService -> commit)");
        }

        //Check whether root dir is exist or not
        File rootDir = HubzContext.getRootDir();
        if(rootDir ==null || !HubzContext.isInitialized()){
            throw new RepositoryNotFoundException("Base directory not set. Use 'reset' or restart HubZ to choose one. " +
                    "(CommitService -> commit)");
        }

        //Check whether HubZDir is initialized or not
        File hubzDir = new File(rootDir, HubzPath.HUBZ_DIR);
        if( hubzDir==null || !hubzDir.exists()){
            throw new RepositoryNotFoundException("No repository found. Run 'init' first. (CommitService -> commit)");
        }
        try{
            RepositoryHelper helper = new RepositoryHelper();

            //Get Meta File
            MetaModel meta = helper.loadMeta();

            //Get Old Index File
            IndexModel oldIndex = helper.loadIndex();

            //Scanning entire folder structure to store in new Index File without storing hash
            IndexModel newIndex = new IndexModel();
            helper.scanWorkingDirectory(rootDir,newIndex);

            //Finding created, modified, or deleted files in current folder
            //Map<relative path, absolute path>
            Map<String, String> created = new LinkedHashMap<>();
            Map<String, String> modified = new LinkedHashMap<>();
            Map<String, String> deleted = new LinkedHashMap<>();

            helper.calculateWorkingDirectoryDiff(oldIndex, newIndex, created, modified, deleted);

            if (created.isEmpty() && modified.isEmpty() && deleted.isEmpty()) {
                return new OperationResult(true, "No changes detected. Working directory is up to date.");
            }

            //Recording blobs and its hash
            //Map<relative path, blob hash>
            Map<String, TreeEntry> blobs = new LinkedHashMap<>();

            //Saving blob for newly created and modified files
            for(String path : created.keySet()){
                String absolutePath = created.get(path);

                String newBlobHash = JsonSerializer.saveBlob(absolutePath);

                //Setting blob hash in index model
                newIndex.getFiles().get(path).setHash(newBlobHash);
                TreeEntry treeEntry = new TreeEntry();
                treeEntry.setCreatedBlob(newBlobHash);
                blobs.put(path, treeEntry);
            }

            for(String path : modified.keySet()){
                String absolutePath = modified.get(path);
                String newBlobHash = JsonSerializer.saveBlob(absolutePath);

                //Setting blob hash in index model
                newIndex.getFiles().get(path).setHash(newBlobHash);

                String oldBlobHash = oldIndex.getFiles().get(path).getHash();
                blobs.put(path,new TreeEntry(oldBlobHash,newBlobHash));
            }

            //Storing deleted file and its hash in blob itself
            for(String path : deleted.keySet()){
                String oldBlobHash = oldIndex.getFiles().get(path).getHash();
                TreeEntry treeEntry = new TreeEntry();
                treeEntry.setDeletedBlob(oldBlobHash);
                blobs.put(path,treeEntry);
            }

            //Creating new tree model for current commit
            TreeModel tree = new TreeModel();
            tree.setFiles(blobs);

            //Saving Tree Blob in tree dir
            String treeHash = JsonSerializer.saveTree(tree);
            tree.setHash(treeHash);

            //Now getting parent commit
            File branchRefFile = new File(hubzDir, helper.getBranchPath());
            String parentHash = helper.getHeadCommitHash();

            int commitNumber = meta.getCommitCount()+1;
            //Creating commit model and storing it in commit dir
            CommitModel commit = new CommitModel();
            commit.setCommitNumber(commitNumber);
            commit.setAuthor(HubzContext.getAuthor());
            commit.setParent(parentHash);
            commit.setTreeHash(treeHash);
            commit.setTimestamp(TimeUtil.getCurrentTimestamp());
            commit.setMessage(message);
            commit.setBranchName(HubzContext.getCurrentBranchName());
            String commitHash = JsonSerializer.saveCommit(commit);

            //Updating hash in current branch file at branch\refs dir
            FileManager.writeFile(branchRefFile.getAbsolutePath(), commitHash);

            //Updating meta file and storing updated commit count
            meta.setCommitCount(commitNumber);
            JsonSerializer.saveMeta(meta);

            //Updating graph file and storing graph path
            JsonSerializer.updateGraph(commitHash, parentHash);

            //Storing new index model at index file
            JsonSerializer.saveIndex(newIndex);

            //Storing snapshot of entire folder structure or index file for every 50 commits
            if (commitNumber % 50 == 0 || commitNumber==1) {
                //Getting snapshot file path
                String snapshotRelPath = HubzPath.getSnapshotFileName(commitNumber);
                File snapshotFile = new File(rootDir, snapshotRelPath);

                JsonSerializer.writeJsonFile(snapshotFile, newIndex);

                //Updating cluster file with new snapshot file path, timestamp, commitHash
                ClusterModel cluster;
                File clusterFile = new File(rootDir, HubzPath.CLUSTER_FILE);
                if (FileManager.exists(clusterFile.getAbsolutePath())) {
                    cluster = JsonSerializer.readJsonFile(clusterFile, ClusterModel.class);
                    if (cluster == null) cluster = new ClusterModel();
                } else {
                    cluster = new ClusterModel();
                }

                SnapshotInfo si = new SnapshotInfo(commitHash, snapshotRelPath, TimeUtil.getCurrentTimestamp());
                cluster.getSnapshots().add(si);

                JsonSerializer.saveCluster(cluster);
            }

            return new OperationResult(true,
                    "Commit " + commit.getCommitNumber() + " created successfully!\n" +
                            "Hash: " + commitHash + "\n" +
                            "Created: " + created.size() +
                            " | Modified: " + modified.size() +
                            " | Deleted: " + deleted.size());
        }
        catch (IOException e){
//            e.printStackTrace();
            return new OperationResult(false,"IO error: "+e.getMessage());
        }
    }
}
