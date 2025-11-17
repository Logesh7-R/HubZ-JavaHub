package hubz.core.service;

import com.google.gson.reflect.TypeToken;
import hubz.context.HubzContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.io.FileManager;
import hubz.io.JsonSerializer;
import hubz.model.commitmodel.CommitModel;
import hubz.model.indexmodel.IndexEntry;
import hubz.model.indexmodel.IndexModel;
import hubz.model.metamodel.MetaModel;
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

        RepositoryHelper helper = new RepositoryHelper();
        String head = helper.getHeadCommitHash();

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
}

