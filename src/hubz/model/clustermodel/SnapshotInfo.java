package hubz.model.clustermodel;

public class SnapshotInfo {
    private String commit;//commit hash
    private String path;//snapshots/index-100.json
    private String timestamp;//TimeStamp

    public SnapshotInfo() {}

    public SnapshotInfo(String commit, String path, String timestamp) {
        this.commit = commit;
        this.path = path;
        this.timestamp = timestamp;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
