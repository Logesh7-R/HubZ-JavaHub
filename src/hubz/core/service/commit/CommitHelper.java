package hubz.core.service.commit;

import hubz.core.HubZContext;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.model.indexmodel.IndexEntry;
import hubz.core.model.indexmodel.IndexModel;
import hubz.core.util.FileUtil;
import hubz.core.util.HubZPath;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CommitHelper {

     void scanFileRecursive(File currentDir, IndexModel index){
        File[] files = currentDir.listFiles();
        if(files==null) return;

        File rootDir = HubZContext.getRootDir();
        File hubzDir = new File(rootDir, HubZPath.HUBZ_DIR);

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

    void detectChanges(IndexModel oldIndex, IndexModel newIndex,
                               Map<String, String> created, Map<String, String> modified,
                               Map<String, String> deleted){

        //Finding Modified or Created files
        for(Map.Entry<String, IndexEntry> entry : newIndex.getFiles().entrySet()){
            String path = entry.getKey();
            IndexEntry newFile = entry.getValue();
            IndexEntry oldFile = oldIndex.getFiles().get(path);

            if(oldFile==null){
                created.put(path, new File(HubZContext.getRootDir(), path).getAbsolutePath());
            }
            else if(oldFile.getSize() != newFile.getSize()
                    || oldFile.getMtime() != newFile.getMtime()){
                modified.put(path,new File(HubZContext.getRootDir(), path).getAbsolutePath());
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
                deleted.put(path,new File(HubZContext.getRootDir(), path).getAbsolutePath());
            }
        }
    }

    String getBranchPath() throws RepositoryNotFoundException, IOException {
        File headFile = new File(HubZContext.getRootDir(), HubZPath.HEAD_FILE);
        System.out.println(headFile);
        String headContent = FileUtil.readFile(headFile.getAbsolutePath()).trim();
        String branchPath = null;
        if (headContent.startsWith("ref:")) {
            branchPath = headContent.substring(4).trim();
        } else {
            throw new RepositoryNotFoundException("Invalid HEAD reference. Please check repository state.");
        }
        return branchPath;
    }
}
