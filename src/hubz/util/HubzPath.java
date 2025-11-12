package hubz.util;

import java.io.File;

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

    public static final String SNAPSHOTS_DIR = HUBZ_DIR + File.separator + "snapshots";
    public static final String TEMP_DIR = HUBZ_DIR + File.separator + "temp";

    public static String getSnapshotFileName(int commitCount) {
        return SNAPSHOTS_DIR + File.separator + "index-" + commitCount + ".json";
    }
}
