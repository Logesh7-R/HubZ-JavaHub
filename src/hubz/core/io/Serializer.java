package hubz.core.io;

import com.google.gson.reflect.TypeToken;
import hubz.core.HubZContext;
import hubz.core.model.clustermodel.ClusterModel;
import hubz.core.model.commitmodel.CommitModel;
import hubz.core.model.indexmodel.IndexModel;
import hubz.core.model.metamodel.MetaModel;
import hubz.core.model.treemodel.TreeModel;
import hubz.core.util.FileUtil;
import hubz.core.util.HashUtil;
import hubz.core.util.HubZPath;
import hubz.core.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hubz.core.util.FileUtil.atomicWrite;

public class Serializer {
    private static File rootDir = HubZContext.getRootDir();
    private Serializer(){}

    public static String saveBlob(String filePath) throws IOException {

        String blobDir = new File(rootDir, HubZPath.BLOBS_DIR).getAbsolutePath();
        String blobHash = HashUtil.sha256File(filePath);
        String blobPath = blobDir+File.separator+blobHash+".txt";

        if(!FileUtil.exists(blobPath)){
            String content = FileUtil.readFile(filePath);
            FileUtil.createFile(blobPath, content);
        }
        return blobHash;
    }

    public static String saveTree( TreeModel tree) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        String treeDir = new File(rootDir,HubZPath.TREES_DIR).getAbsolutePath();
        String json = JsonUtil.toJson(tree);
        String treeHash = HashUtil.sha256String(json);
        String TreePath = treeDir+File.separator+treeHash+".txt";
        if(!FileUtil.exists(TreePath)){
            FileUtil.createFile(TreePath, json);
        }
        tree.setHash(treeHash);
        return treeHash;
    }

    public static String saveCommit(CommitModel commit) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        String commitDir = new File(rootDir,HubZPath.COMMITS_DIR).getAbsolutePath();
        String json = JsonUtil.toJson(commit);
        String commitHash = HashUtil.sha256String(json);
        String commitPath = commitDir+File.separator+ commitHash +".txt";
        if(!FileUtil.exists(commitPath)){
            FileUtil.createFile(commitPath, json);
        }
        commit.setHash(commitHash);
        return commitHash;
    }

    public static void saveIndex(IndexModel index) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        File indexFile = new File(rootDir, HubZPath.INDEX_FILE);
        String json = JsonUtil.toJson(index);
        FileUtil.atomicWrite(indexFile, json);
    }

public static void saveCluster(ClusterModel cluster) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        File clusterFile = new File(rootDir, HubZPath.CLUSTER_FILE);
        String json = JsonUtil.toJson(cluster);
        FileUtil.atomicWrite(clusterFile, json);
    }

    public static void saveMeta(MetaModel meta) throws IOException {
//        File baseDir = new File(HubZContext.getRootDir(), HubZPath.HUBZ_DIR);
        File metaFile = new File(rootDir, HubZPath.META_FILE);
        String json = JsonUtil.toJson(meta);
        FileUtil.atomicWrite(metaFile, json);
    }

    public static void updateGraph(String commitHash, String parentHash) throws IOException {
        File graphFile = new File(rootDir, HubZPath.GRAPH_FILE);

        Map<String, List<String>> graph = new LinkedHashMap<>();

        if (FileUtil.exists(graphFile.getAbsolutePath())) {
            String json = FileUtil.readFile(graphFile.getAbsolutePath());
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

        FileUtil.atomicWrite(graphFile, JsonUtil.toJson(graph));
    }

    public static <T> T readJsonFile(File file, Class<T> clazz) throws IOException {
        String json = FileUtil.readFile(file.getAbsolutePath());
        return JsonUtil.fromJson(json, clazz);
    }

    public static void writeJsonFile(File file, Object obj) throws IOException {
        String json = JsonUtil.toJson(obj);
        atomicWrite(file, json);
    }
}
