package hubz.core.model.clustermodel;

import java.util.ArrayList;
import java.util.List;

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
