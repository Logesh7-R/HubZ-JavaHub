package hubz.core.io;

import hubz.core.HubZContext;
import hubz.core.exception.RepositoryInitException;
import hubz.core.util.FileUtil;
import hubz.core.util.HubZPath;

import java.io.File;
import java.io.IOException;

public class FileManager {

    public void initializeRepoStructure(File rootDir) throws RepositoryInitException {
        try {
            String base = new File(rootDir,HubZPath.HUBZ_DIR).getAbsolutePath();

            FileUtil.createDir(base);
            FileUtil.createDir(new File(rootDir, HubZPath.OBJECTS_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.BLOBS_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.TREES_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.COMMITS_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.REFS_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.BRANCHES_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.SNAPSHOTS_DIR).getAbsolutePath());
            FileUtil.createDir(new File(rootDir, HubZPath.TEMP_DIR).getAbsolutePath());
            FileUtil.createFile(new File(rootDir,HubZPath.GRAPH_FILE).getAbsolutePath(), "{}");
            FileUtil.createFile(new File(rootDir,HubZPath.INDEX_FILE).getAbsolutePath(), "{}");
            FileUtil.createFile(new File(rootDir,HubZPath.CLUSTER_FILE).getAbsolutePath(), "{\"snapshots\": []}");
            FileUtil.createFile(new File(rootDir,HubZPath.META_FILE).getAbsolutePath(), "{\"commitCount\": 0, \"branch\": \"main\", \"author\":\""+ HubZContext.getAuthor() +"\"}");

            FileUtil.createFile(new File(rootDir,HubZPath.HEAD_FILE).getAbsolutePath(), "ref: refs" + File.separator + "branches" + File.separator + "main");
            FileUtil.createFile(base + File.separator + "refs" + File.separator + "branches" + File.separator + "main", "");

        } catch (IOException e) {
            throw new RepositoryInitException("Error while creating .hubz structure: " + e.getMessage());
        }
    }
}
