package hubz.model.treemodel;

import java.util.LinkedHashMap;
import java.util.Map;

public class TreeModel {
    transient private String hash;
    private Map<String, TreeEntry> files = new LinkedHashMap<>(); //Relative Filepath -> old blob hash and new blob hash
    public TreeModel() {}

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Map<String, TreeEntry> getFiles() {
        return files;
    }

    public void setFiles(Map<String, TreeEntry> files) {
        this.files = files;
    }
}
