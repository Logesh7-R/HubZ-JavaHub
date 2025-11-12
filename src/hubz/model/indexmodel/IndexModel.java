package hubz.model.indexmodel;

import java.util.LinkedHashMap;
import java.util.Map;

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
