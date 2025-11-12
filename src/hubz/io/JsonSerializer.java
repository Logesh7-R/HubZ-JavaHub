package hubz.io;

import com.google.gson.reflect.TypeToken;
import hubz.context.HubzContext;
import hubz.model.clustermodel.ClusterModel;
import hubz.model.commitmodel.CommitModel;
import hubz.model.indexmodel.IndexModel;
import hubz.model.metamodel.MetaModel;
import hubz.model.treemodel.TreeModel;
import hubz.util.HashUtil;
import hubz.util.HubzPath;
import hubz.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hubz.io.FileManager.atomicWrite;

public class JsonSerializer {
    private static File rootDir = HubzContext.getRootDir();
    private JsonSerializer(){}

    public static String saveBlob(String filePath) throws IOException {

        String blobDir = new File(rootDir, HubzPath.BLOBS_DIR).getAbsolutePath();
        String blobHash = HashUtil.sha256File(filePath);
        String blobPath = blobDir+File.separator+blobHash+".txt";

        if(!FileManager.exists(blobPath)){
            String content = FileManager.readFile(filePath);
            FileManager.createFile(blobPath, content);
        }
        return blobHash;
    }

    public static String saveTree( TreeModel tree) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        String treeDir = new File(rootDir, HubzPath.TREES_DIR).getAbsolutePath();
        String json = JsonUtil.toJson(tree);
        String treeHash = HashUtil.sha256String(json);
        String TreePath = treeDir+File.separator+treeHash+".txt";
        if(!FileManager.exists(TreePath)){
            FileManager.createFile(TreePath, json);
        }
        tree.setHash(treeHash);
        return treeHash;
    }

    public static String saveCommit(CommitModel commit) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        String commitDir = new File(rootDir, HubzPath.COMMITS_DIR).getAbsolutePath();
        String json = JsonUtil.toJson(commit);
        String commitHash = HashUtil.sha256String(json);
        String commitPath = commitDir+File.separator+ commitHash +".txt";
        if(!FileManager.exists(commitPath)){
            FileManager.createFile(commitPath, json);
        }
        commit.setHash(commitHash);
        return commitHash;
    }

    public static void saveIndex(IndexModel index) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        File indexFile = new File(rootDir, HubzPath.INDEX_FILE);
        String json = JsonUtil.toJson(index);
        FileManager.atomicWrite(indexFile, json);
    }

public static void saveCluster(ClusterModel cluster) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        File clusterFile = new File(rootDir, HubzPath.CLUSTER_FILE);
        String json = JsonUtil.toJson(cluster);
        FileManager.atomicWrite(clusterFile, json);
    }

    public static void saveMeta(MetaModel meta) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        File metaFile = new File(rootDir, HubzPath.META_FILE);
        String json = JsonUtil.toJson(meta);
        FileManager.atomicWrite(metaFile, json);
    }

    public static void updateGraph(String commitHash, String parentHash) throws IOException {
        File graphFile = new File(rootDir, HubzPath.GRAPH_FILE);

        Map<String, List<String>> graph = new LinkedHashMap<>();

        if (FileManager.exists(graphFile.getAbsolutePath())) {
            String json = FileManager.readFile(graphFile.getAbsolutePath());
            if (!json.trim().isEmpty()) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                graph = JsonUtil.fromJson(json, type);
            }
        }

        if (!graph.containsKey(commitHash)) {
            graph.put(commitHash, new ArrayList<>());
        }

        if (parentHash != null) {
            graph.get(commitHash).add(parentHash);
        }

        FileManager.atomicWrite(graphFile, JsonUtil.toJson(graph));
    }

    public static <T> T readJsonFile(File file, Class<T> clazz) throws IOException {
        String json = FileManager.readFile(file.getAbsolutePath());
        return JsonUtil.fromJson(json, clazz);
    }

    public static void writeJsonFile(File file, Object obj) throws IOException {
        String json = JsonUtil.toJson(obj);
        atomicWrite(file, json);
    }
}
