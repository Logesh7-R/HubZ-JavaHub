package hubz.core.service.commitservice;

import hubz.context.HubzContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.model.indexmodel.IndexEntry;
import hubz.model.indexmodel.IndexModel;
import hubz.io.FileManager;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;
import java.util.Map;

//Helper class for commit service class
public class CommitHelper {

    //Scan folder structure and store it in index object
     void scanFileRecursive(File currentDir, IndexModel index){
        File[] files = currentDir.listFiles();
        if(files==null) return;

        File rootDir = HubzContext.getRootDir();
        File hubzDir = new File(rootDir, HubzPath.HUBZ_DIR);

        for(File file : files){
            if(file.getAbsolutePath().startsWith(hubzDir.getAbsolutePath()))
                continue;

            if(file.isDirectory()){
                scanFileRecursive(file, index);
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
    void detectChanges(IndexModel oldIndex, IndexModel newIndex,
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
                    || oldFile.getMtime() == newFile.getMtime()){
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

    //Getting current working branch
    String getBranchPath() throws RepositoryNotFoundException, IOException {
        File headFile = new File(HubzContext.getRootDir(), HubzPath.HEAD_FILE);
        String headContent = FileManager.readFile(headFile.getAbsolutePath()).trim();
        String branchPath = null;
        if (headContent.startsWith("ref:")) {
            branchPath = headContent.substring(4).trim();
        } else {
            throw new RepositoryNotFoundException("Invalid HEAD reference. Please check repository state. " +
                    "(CommitHelper -> getBranch())");
        }
        return branchPath;
    }
}
