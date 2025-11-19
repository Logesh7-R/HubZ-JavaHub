package hubz.core.operations;

import hubz.core.exception.RepositoryNotFoundException;

import hubz.core.service.logservice.CommitLogService;
import hubz.model.OperationResult;

public class LogOperation implements Operation {

    @Override
    public OperationResult execute(String arg) {

        try {
            int limit = 25;  // default
            if (arg != null) {
                try { limit = Integer.parseInt(arg.trim()); }
                catch (NumberFormatException ignored) {}
            }
            CommitLogService service = new CommitLogService();
            var logs = service.loadCommitLogs(limit);

            StringBuilder all = new StringBuilder();
            for (String s : logs) {
                all.append(s).append("\n");
            }

            return new OperationResult(true, all.toString());

        } catch (RepositoryNotFoundException e) {
            return new OperationResult(false, e.getMessage());
        } catch (Exception e) {
            return new OperationResult(false, "Log failed: " + e.getMessage());
        }
    }
}
