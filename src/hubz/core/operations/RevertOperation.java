package hubz.core.operations;

import hubz.core.service.revertservice.RevertService;
import hubz.model.RevertResult;

public class RevertOperation {

    public RevertResult execute(String targetCommitHash) {
        try {
            RevertService service = new RevertService();
            return service.revert(targetCommitHash);
        } catch (Exception e) {
            return new RevertResult(
                    RevertResult.Status.ABORTED,
                    targetCommitHash,
                    null,
                    null,
                    "Revert failed: " + e.getMessage()
            );
        }
    }
}
