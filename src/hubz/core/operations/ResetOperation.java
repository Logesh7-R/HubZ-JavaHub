package hubz.core.operations;

import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.service.resetservice.ResetService;
import hubz.model.OperationResult;

public class ResetOperation implements Operation {

    private final ResetService resetService = new ResetService();

    @Override
    public OperationResult execute(String arg) {

        if (arg == null || arg.trim().isEmpty()) {
            return usage("Missing arguments");
        }

        String[] tokens = arg.trim().split("\\s+");

        boolean flagHard = false;
        boolean flagUndo = false;
        String commitHash = null;

        for (String raw : tokens) {

            String token = raw.trim();

            if (token.equalsIgnoreCase("--hard")) {
                flagHard = true;
            }
            else if (token.equalsIgnoreCase("--undo")) {
                flagUndo = true;
            }
            else if (token.startsWith("--")) {
                // Unknown flag
                return usage("Unknown flag: " + token);
            }
            else {
                // A valid hash (non-flag)
                if (commitHash == null) commitHash = token;
                else return usage("Multiple hashes detected: '" + commitHash + "', '" + token + "'");
            }
        }

        try {
            if (flagUndo) {

                if (commitHash != null) {
                    return usage("Undo mode does not accept a commit hash");
                }

                // undo hard
                if (flagHard) {
                    return resetService.undoReset(true);
                }

                // undo soft
                return resetService.undoReset(false);
            }

            // Case: Normal or hard reset
            if (commitHash == null || commitHash.trim().isEmpty()) {
                return usage("Missing commit hash");
            }

            if (flagHard) {
                return resetService.resetHard(commitHash, "hard");
            }

            return resetService.reset(commitHash, "soft");
        }
        catch (RepositoryNotFoundException e) {
            return new OperationResult(false, e.getMessage());
        }
        catch (Exception e) {
            return new OperationResult(false, "Reset failed: " + e.getMessage());
        }
    }

    private OperationResult usage(String msg) {
        return new OperationResult(
                false,
                msg + "\n" +
                        "Usage:\n" +
                        "  reset <hash>\n" +
                        "  reset --hard <hash>\n" +
                        "  reset --undo\n" +
                        "  reset --undo --hard\n"
        );
    }
}
