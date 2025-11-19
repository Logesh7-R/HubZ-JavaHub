package hubz.model.clustermodel;

public class SnapshotInfo {
    private String commitHash;//commit hash
    private String path;//snapshots/index-100.json
    private String timestamp;//TimeStamp
    private int commitNumber;

    public SnapshotInfo() {}

    public SnapshotInfo(String commitHash, String path, String timestamp, int commitNumber) {
        this.commitHash = commitHash;
        this.path = path;
        this.timestamp = timestamp;
        this.commitNumber = commitNumber;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
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

    public int getCommitNumber(){
        return commitNumber;
    }

    public void setCommitNumber(int commitNumber) {
        this.commitNumber = commitNumber;
    }
}
