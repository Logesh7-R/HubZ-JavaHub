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
import hubz.model.treemodel.TreeEntry;
import hubz.model.treemodel.TreeModel;
import hubz.util.HubzPath;
import hubz.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class RepositoryHelper {
    public void scanWorkingDirectory(File currentDir, IndexModel index){
        File[] files = currentDir.listFiles();
        if(files==null) return;

        File rootDir = HubzContext.getRootDir();
        File hubzDir = new File(rootDir, HubzPath.HUBZ_DIR);

        for(File file : files){
            if(file.getAbsolutePath().startsWith(hubzDir.getAbsolutePath()))
                continue;

            if(file.isDirectory()){
                scanWorkingDirectory(file, index);
            }
            else{
                String relativePath = rootDir.toPath().relativize(file.toPath()).toString();
                IndexEntry entry = new IndexEntry(
                        "",
                        file.length(),
                        file.lastModified());
                index.getFiles().put(relativePath,entry);
            }
        }
    }

    //Detect files which are modified, created and deleted
    //NewIndex will store unmodified files and its blob hash
    public void calculateWorkingDirectoryDiff(IndexModel oldIndex, IndexModel newIndex,
                              Map<String, String> created, Map<String, String> modified,
                              Map<String, String> deleted){

        //Finding Modified or Created files
        for(Map.Entry<String, IndexEntry> entry : newIndex.getFiles().entrySet()){
            String path = entry.getKey();
            IndexEntry newFile = entry.getValue();
            IndexEntry oldFile = oldIndex.getFiles().get(path);

            if(oldFile==null){
                created.put(path, new File(HubzContext.getRootDir(), path).getAbsolutePath());
            }
            else if(oldFile.getSize() != newFile.getSize()
                    || oldFile.getMtime() != newFile.getMtime()){
                modified.put(path,new File(HubzContext.getRootDir(), path).getAbsolutePath());
            }
            else if(oldFile.getSize() == newFile.getSize()
                    && oldFile.getMtime() == newFile.getMtime()){
                newIndex.getFiles().get(path).setHash(oldFile.getHash());
            }
        }

        //Finding Deleted Files
        for(Map.Entry<String, IndexEntry> entry : oldIndex.getFiles().entrySet()){
            String path = entry.getKey();
            if(!newIndex.getFiles().containsKey(path)){
                deleted.put(path,new File(HubzContext.getRootDir(), path).getAbsolutePath());
            }
        }
    }

    //Getting current working branch name
    public String getCurrentBranchName() throws RepositoryNotFoundException, IOException {
        File headFile = new File(HubzContext.getRootDir(), HubzPath.HEAD_FILE);
        String headContent = FileManager.readFile(headFile.getAbsolutePath()).trim();
        String currentBranch[] = null;
        if (headContent.startsWith("ref:")) {
           currentBranch = headContent.split(File.separator);
        } else {
            throw new RepositoryNotFoundException("Invalid HEAD reference. Please check repository state. " +
                    "(ServiceHelper -> getBranch())");
        }
        return currentBranch[currentBranch.length-1];
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
        File hubzDir = new File(HubzContext.getRootDir(),HubzPath.HUBZ_DIR);
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
        File graphFile = new File(HubzContext.getRootDir(), HubzPath.GRAPH_FILE);

        String json = FileManager.readFile(graphFile.getAbsolutePath());
        Type type = new TypeToken<LinkedHashMap<String, List<String>>>(){}.getType();

        LinkedHashMap<String, List<String>> graph = JsonUtil.fromJson(json, type);
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
            File currentBlobFile = new File(repoRoot, HubzPath.BLOBS_DIR +File.separator+ currentBlobHash + ".txt");
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
            File oldBlobFile = new File(repoRoot, HubzPath.BLOBS_DIR +File.separator+ oldBlobHash + ".txt");
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

    public IndexModel buildTargetIndex(String targetCommitHash) throws IOException, RepositoryNotFoundException {
        List<String> history = traverseGraphFromHead();
        if (history == null || history.isEmpty() || !history.contains(targetCommitHash)) {
            return null;
        }

        File targetCommitFile = HubzPath.getCommitFilePath(targetCommitHash);
        CommitModel targetCommit = JsonSerializer.readJsonFile(targetCommitFile, CommitModel.class);
        SnapshotInfo nearestSnapShot = getNearestSnapShot(targetCommit);
        List<String> graphPaths = getShortestPath(targetCommitHash,nearestSnapShot.getCommit());
        IndexModel targetIndex = JsonSerializer.readJsonFile(new File(nearestSnapShot.getPath()), IndexModel.class);
        Map<String, IndexEntry> targetFileStructure = targetIndex.getFiles();
        for(String commitHash : graphPaths){
            CommitModel commitModel = JsonSerializer.readJsonFile(HubzPath.getCommitFilePath(commitHash), CommitModel.class);
            TreeModel treeModel = JsonSerializer.readJsonFile(HubzPath.getTreeFilePath(commitModel.getTreeHash()), TreeModel.class);
            Map<String,TreeEntry> treeFiles = treeModel.getFiles();

            for(String path : treeFiles.keySet()){
                TreeEntry treeEntry = treeFiles.get(path);

                if(treeEntry.isCreated()||treeEntry.isModified()){
                    IndexEntry index = new IndexEntry();
                    index.setHash(treeEntry.getCreatedBlob());
                    index.setSize(treeEntry.getSize());
                    index.setMtime(treeEntry.getMtime());
                    targetFileStructure.put(path,index);
                }
                else if(treeEntry.isDeleted()){
                    targetFileStructure.remove(path);
                }

            }
        }
        targetIndex.setFiles(targetFileStructure);
        return targetIndex;
    }

    public SnapshotInfo getNearestSnapShot(CommitModel targetCommit) throws IOException {
        int targetCommitNumber = targetCommit.getCommitNumber();
        SnapshotInfo nearestSnapShot = null;
        ClusterModel cluster;
        File clusterFile = new File(HubzContext.getRootDir(), HubzPath.CLUSTER_FILE);
        if (FileManager.exists(clusterFile.getAbsolutePath())) {
            cluster = JsonSerializer.readJsonFile(clusterFile, ClusterModel.class);
            if (cluster == null) cluster = new ClusterModel();
        } else {
            cluster = new ClusterModel();
        }

        for(SnapshotInfo si : cluster.getSnapshots()){
            if(nearestSnapShot==null){
                nearestSnapShot =si;
            }else{
                if(si.getCommitNumber()<=targetCommitNumber){
                    if(nearestSnapShot.getCommitNumber()>si.getCommitNumber()){
                        nearestSnapShot = si;
                    }
                }
            }
        }
        return nearestSnapShot;
    }

    public List<String> getShortestPath(String startHash,String targetHash) throws IOException {
        File graphFile = new File(HubzContext.getRootDir(), HubzPath.GRAPH_FILE);

        String json = FileManager.readFile(graphFile.getAbsolutePath());
        Type type = new TypeToken<LinkedHashMap<String, List<String>>>(){}.getType();

        LinkedHashMap<String, List<String>> graph = JsonUtil.fromJson(json, type);
        if (graph == null) return Collections.emptyList();

        if (startHash.equals(targetHash)) {
            return Collections.singletonList(startHash);
        }

        Queue<String> queue = new LinkedList<>();
        queue.add(startHash);

        Map<String,String> parent = new LinkedHashMap<>();
        parent.put(startHash,null);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            List<String> neighbors = graph.getOrDefault(current, Collections.emptyList());

            for (String next : neighbors) {
                if (!parent.containsKey(next)) {
                    parent.put(next, current);
                    queue.add(next);

                    if (next.equals(targetHash)) {
                        return buildPath(parent, targetHash);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> buildPath(Map<String,String>parent,String targetHash){
        List<String> path = new ArrayList<>();
        String node = targetHash;

        while (node != null) {
            path.add(node);
            node = parent.get(node);
        }

        Collections.reverse(path);
        return path;
    }

    public void buildFolder(IndexModel buildingIndex) throws IOException {
        IndexModel currentIndex = new IndexModel();
        scanWorkingDirectory(HubzContext.getRootDir(), currentIndex);

        List<String> deletingFiles = new LinkedList<>();
        List<String> creatingFiles = new LinkedList<>();
        List<String> modifyingFiles = new LinkedList<>();

        Map<String, IndexEntry> buildingFolder = buildingIndex.getFiles();
        Map<String, IndexEntry> currentFolder = currentIndex.getFiles();
        for(String path: buildingFolder.keySet()){
            if(!currentFolder.containsKey(path)){
                creatingFiles.add(path);
            }
            else if(!buildingFolder.get(path).getHash().equals(currentFolder.get(path).getHash())){
                modifyingFiles.add(path);
            }
        }

        for(String path: currentFolder.keySet()){
            if(!buildingFolder.containsKey(path)){
                deletingFiles.add(path);
            }
        }

        for(String path:creatingFiles){
            String blobHash = buildingFolder.get(path).getHash();
            File blobFile = HubzPath.getBlobFilePath(blobHash);
            File workingFile = new File(HubzContext.getRootDir(), path);
            String content = FileManager.readFile(blobFile.getAbsolutePath());
            FileManager.createDir(workingFile.getParent());
            FileManager.createFile(workingFile.getAbsolutePath(), content);
        }

        for(String path:modifyingFiles){
            String blobHash = buildingFolder.get(path).getHash();
            File blobFile = HubzPath.getBlobFilePath(blobHash);
            File workingFile = new File(HubzContext.getRootDir(), path);
            String content = FileManager.readFile(blobFile.getAbsolutePath());
            FileManager.atomicWrite(workingFile, content);
        }

        for(String path:deletingFiles){
            File workingFile = new File(HubzContext.getRootDir(), path);
            FileManager.deleteFileAndCleanParents(workingFile);
        }
    }

    public void setTerminatedSnapshotPath(String targetCommitHash, List<String> terminatedSnapshotPath) throws RepositoryNotFoundException, IOException {
        String currentCommitHash = getHeadCommitHash();
        File currentCommitPath = HubzPath.getCommitFilePath(currentCommitHash);
        CommitModel currentCommitModel = JsonSerializer.readJsonFile(currentCommitPath, CommitModel.class);

        File targetCommitPath = HubzPath.getCommitFilePath(targetCommitHash);
        CommitModel targetCommitModel = JsonSerializer.readJsonFile(targetCommitPath, CommitModel.class);

        int tmp1 = (targetCommitModel.getCommitNumber()+1)/50;
        int tmp2 = (currentCommitModel.getCommitNumber()-1)/50;

        int start = Math.min(tmp1, tmp2);
        int end = Math.max(tmp1,tmp2);

        for(int i = start;i<=end;i++){
            if(i!=0){
                terminatedSnapshotPath.add(HubzPath.getSnapshotFileName(i*50));
            }
        }
    }
}

