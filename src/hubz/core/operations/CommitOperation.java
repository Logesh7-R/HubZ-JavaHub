package hubz.core.operations;

import hubz.core.exception.InvalidCommitMessageException;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.model.OperationResult;
import hubz.core.service.commit.CommitService;

public class CommitOperation implements Operation{
    @Override
    public OperationResult execute(String args) {
        try {
            CommitService commitService = new CommitService();
            return commitService.commit(args);
        } catch (InvalidCommitMessageException | RepositoryNotFoundException e) {
            return new OperationResult(false, e.getMessage());
        } catch (Exception e) {
            return new OperationResult(false, "Unexpected error: " + e.getMessage());
        }
    }

}
