package hubz.core.service.commit;

import hubz.core.HubZContext;
import hubz.core.exception.InvalidCommitMessageException;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.io.Serializer;
import hubz.core.model.*;
import hubz.core.model.clustermodel.ClusterModel;
import hubz.core.model.clustermodel.SnapshotInfo;
import hubz.core.model.commitmodel.CommitModel;
import hubz.core.model.indexmodel.IndexModel;
import hubz.core.model.metamodel.MetaModel;
import hubz.core.model.treemodel.TreeModel;
import hubz.core.util.FileUtil;
import hubz.core.util.HubZPath;
import hubz.core.util.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommitService {
    public OperationResult commit(String message) throws InvalidCommitMessageException, RepositoryNotFoundException {
        //Check message is valid or not
        if (message == null || message.trim().isEmpty()) {
            throw new InvalidCommitMessageException("Commit message cannot be empty.");
        }
        //Check whether root dir is exist or not
        File repoDir = HubZContext.getRootDir();
        if(repoDir==null || !repoDir.exists()){
            throw new RepositoryNotFoundException("Base directory not set. Use 'reset' or restart HubZ to choose one.");
        }

        //Check whether HubZDir is initialized or not
        File hubzDir = new File(repoDir, HubZPath.HUBZ_DIR);
        if( hubzDir==null || !hubzDir.exists()){
            throw new RepositoryNotFoundException("No repository found. Run 'init' first.");
        }
        try{
            CommitHelper helper = new CommitHelper();
            //Get Meta File
            MetaModel meta;
            File metaFile = new File(repoDir,HubZPath.META_FILE);
            if(!metaFile.exists()){
                meta = new MetaModel(0,"main",HubZContext.getAuthor());
            } else {
                meta = Serializer.readJsonFile(metaFile, MetaModel.class);
                if(meta == null) meta = new MetaModel(0,"main",HubZContext.getAuthor());
            }
            //Get Old Index File
            IndexModel oldIndex;
            File oldIndexFile = new File(repoDir, HubZPath.INDEX_FILE);
            if(!oldIndexFile.exists()){
                oldIndex = new IndexModel();
            } else {
                oldIndex = Serializer.readJsonFile(oldIndexFile, IndexModel.class);
                if (oldIndex==null) oldIndex = new IndexModel();
            }

            //Scanning entire folder structure to store in new Index File without hash
            IndexModel newIndex = new IndexModel();
            helper.scanFileRecursive(repoDir,newIndex);

            //Finding created, modified, or deleted file
            Map<String, String> created = new LinkedHashMap<>();
            Map<String, String> modified = new LinkedHashMap<>();
            Map<String, String> deleted = new LinkedHashMap<>();

            helper.detectChanges(oldIndex, newIndex, created, modified, deleted);

            if (created.isEmpty() && modified.isEmpty() && deleted.isEmpty()) {
                return new OperationResult(true, "No changes detected. Working directory is up to date.");
            }

            //Recording blobs and its hash
            Map<String,String> blobs = new LinkedHashMap<>();

            //Saving blob for created and modified files
            for(String path : created.keySet()){
                String absolutePath = created.get(path);

                String BlobHash = Serializer.saveBlob(absolutePath);

                //Setting blob hash
                newIndex.getFiles().get(path).setHash(BlobHash);

                blobs.put(path,BlobHash);
            }

            for(String path : modified.keySet()){
                String absolutePath = modified.get(path);
                String BlobHash = Serializer.saveBlob(absolutePath);

                //Setting blob hash
                newIndex.getFiles().get(path).setHash(BlobHash);
                blobs.put(path,BlobHash);
            }

            TreeModel tree = new TreeModel();
            tree.setFiles(blobs);

            //Saving Tree Blob
            String treeHash = Serializer.saveTree(tree);
            tree.setHash(treeHash);

            //Now getting parent commit
            File branchRefFile = new File(hubzDir,helper.getBranchPath());
            String parent = null;
            if (FileUtil.exists(branchRefFile.getAbsolutePath())) {
                parent = FileUtil.readFile(branchRefFile.getAbsolutePath()).trim();
                if (parent.isEmpty()) parent = null;
            }

            int commitNumber = meta.getCommitCount();
            //Creating commit model and storing it
            CommitModel commit = new CommitModel();
            commit.setCommitNumber(commitNumber);
            commit.setAuthor(HubZContext.getAuthor());
            commit.setParent(parent);
            commit.setTree(treeHash);
            commit.setTimestamp(TimeUtil.getCurrentTimestamp());
            commit.setMessage(message);
            commit.setDeletedFiles(deleted);
            String commitHash = Serializer.saveCommit(commit);

            FileUtil.writeFile(branchRefFile.getAbsolutePath(), commitHash);
            meta.setCommitCount(commitNumber+1);
            Serializer.saveMeta(meta);

            Serializer.updateGraph(commitHash, parent);

            Serializer.saveIndex(newIndex);

            if (commitNumber % 50 == 0) {
                String snapshotRelPath = HubZPath.getSnapshotFileName(commitNumber);
                File snapshotFile = new File(repoDir, snapshotRelPath);

                Serializer.writeJsonFile(snapshotFile, newIndex);

                ClusterModel cluster;
                File clusterFile = new File(repoDir, HubZPath.CLUSTER_FILE);
                if (FileUtil.exists(clusterFile.getAbsolutePath())) {
                    cluster = Serializer.readJsonFile(clusterFile, ClusterModel.class);
                    if (cluster == null) cluster = new ClusterModel();
                } else {
                    cluster = new ClusterModel();
                }

                SnapshotInfo si = new SnapshotInfo(commitHash, snapshotRelPath, TimeUtil.getCurrentTimestamp());
                cluster.getSnapshots().add(si);

                Serializer.saveCluster(cluster);
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
