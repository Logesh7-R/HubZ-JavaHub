package hubz.model.indexmodel;

import java.util.LinkedHashMap;
import java.util.Map;

//Used to take snapshot of entire folder structure
//Used to find unmodified files by comparing mtime(last modified time) and size of the file
public class IndexModel {
    private Map<String, IndexEntry> files = new LinkedHashMap<>();

    public IndexModel() {}

    public Map<String, IndexEntry> getFiles() {
        return files;
    }

    public void setFiles(Map<String, IndexEntry> files) {
        this.files = files;
    }

}
