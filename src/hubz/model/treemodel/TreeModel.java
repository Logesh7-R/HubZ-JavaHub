package hubz.model.treemodel;

import java.util.LinkedHashMap;
import java.util.Map;

public class TreeModel {
    transient private String hash;
    private Map<String, String> files = new LinkedHashMap<>(); //Relative Filepath -> blob hash
    public TreeModel() {}

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }
}
