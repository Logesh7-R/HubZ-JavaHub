package hubz.core.service.initservice;

import hubz.context.HubzContext;
import hubz.core.exception.RepositoryInitException;
import hubz.io.FileManager;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;

public class RepoInitializer {

    public void initializeRepoStructure(File rootDir) throws RepositoryInitException {
        try {
            String base = new File(rootDir, HubzPath.HUBZ_DIR).getAbsolutePath();

            FileManager.createDir(base);
            FileManager.createDir(new File(rootDir, HubzPath.OBJECTS_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.BLOBS_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.TREES_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.COMMITS_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.REFS_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.BRANCHES_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.SNAPSHOTS_DIR).getAbsolutePath());
            FileManager.createDir(new File(rootDir, HubzPath.TEMP_DIR).getAbsolutePath());
            FileManager.createFile(new File(rootDir, HubzPath.GRAPH_FILE).getAbsolutePath(), "{}");
            FileManager.createFile(new File(rootDir, HubzPath.INDEX_FILE).getAbsolutePath(), "{}");
            FileManager.createFile(new File(rootDir, HubzPath.CLUSTER_FILE).getAbsolutePath(), "{\"snapshots\": []}");
            FileManager.createFile(new File(rootDir, HubzPath.META_FILE).getAbsolutePath(), "{\"commitCount\": 0, \"branch\": \"main\", \"author\":\""+ HubzContext.getAuthor() +"\"}");

            FileManager.createFile(new File(rootDir, HubzPath.HEAD_FILE).getAbsolutePath(), "ref: refs" + File.separator + "branches" + File.separator + "main");
            FileManager.createFile(base + File.separator + "refs" + File.separator + "branches" + File.separator + "main", "");

        } catch (IOException e) {
            throw new RepositoryInitException("Error while creating .hubz structure: " + e.getMessage());
        }
    }
}
