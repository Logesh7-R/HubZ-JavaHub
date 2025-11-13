package hubz.core.service;

import com.google.gson.reflect.TypeToken;
import hubz.context.HubzContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.io.FileManager;
import hubz.model.indexmodel.IndexEntry;
import hubz.model.indexmodel.IndexModel;
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
}
