package hubz.model.clustermodel;

import java.util.ArrayList;
import java.util.List;

//Used to store snapshot (index file) and its corresponding commit hash for every 50 commits
//index-50.json, index-100.json, etc
public class ClusterModel {
    private List<SnapshotInfo> snapshots = new ArrayList<>();

    public ClusterModel() {}

    public List<SnapshotInfo> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<SnapshotInfo> snapshots) {
        this.snapshots = snapshots;
    }
}
