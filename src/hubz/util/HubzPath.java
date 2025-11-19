package hubz.util;

import hubz.context.HubzContext;

import java.io.File;

//.hubz folder structure
public class HubzPath {

    private HubzPath() {
    }

    public static final String HUBZ_DIR = ".hubz";

    public static final String OBJECTS_DIR = HUBZ_DIR + File.separator + "objects";
    public static final String BLOBS_DIR = OBJECTS_DIR + File.separator + "blob";
    public static final String TREES_DIR = OBJECTS_DIR + File.separator + "tree";
    public static final String COMMITS_DIR = OBJECTS_DIR + File.separator + "commit";

    public static final String REFS_DIR = HUBZ_DIR + File.separator + "refs";
    public static final String BRANCHES_DIR = REFS_DIR + File.separator + "branches";

    public static final String HEAD_FILE = HUBZ_DIR + File.separator + "HEAD";
    public static final String GRAPH_FILE = HUBZ_DIR + File.separator + "graph.json";
    public static final String INDEX_FILE = HUBZ_DIR + File.separator + "index.json";
    public static final String CLUSTER_FILE = HUBZ_DIR + File.separator + "cluster.json";
    public static final String META_FILE = HUBZ_DIR + File.separator + "meta.json";
    public static final String RESET_STACK_FILE = HUBZ_DIR + File.separator + "reset-stack.json";

    public static final String SNAPSHOTS_DIR = HUBZ_DIR + File.separator + "snapshots";
    public static final String TEMP_DIR = HUBZ_DIR + File.separator + "temp";

    //Used to get snapshotFile name
    public static String getSnapshotFileName(int commitCount) {
        return SNAPSHOTS_DIR + File.separator +  "index-"+commitCount +".json";
    }

    public static File getCommitFilePath(String commitHash){
        return new File(HubzContext.getRootDir(),COMMITS_DIR+File.separator+commitHash+".json");
    }

    public static File getTreeFilePath(String treeHash){
        return new File(HubzContext.getRootDir(),TREES_DIR+treeHash+".json");
    }

    public static File getIndexFilePath(){
        return new File(HubzContext.getRootDir(),INDEX_FILE);
    }

    public static File getBlobFilePath(String blobHash){
        return new File(HubzContext.getRootDir(), HubzPath.BLOBS_DIR +File.separator+ blobHash + ".txt");
    }

    public static File getResetStackFilePath(){
        return new File(HubzContext.getRootDir(), RESET_STACK_FILE);
    }

    public static File getClusterFilePath(){
        return new File(HubzContext.getRootDir(),CLUSTER_FILE);
    }
}
