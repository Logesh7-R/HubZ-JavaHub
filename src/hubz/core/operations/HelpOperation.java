package hubz.core.operations;

import hubz.model.OperationResult;

public class HelpOperation implements Operation {

    // ANSI Colors
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String WHITE = "\u001B[37m";

    @Override
    public OperationResult execute(String arg) {

        StringBuilder sb = new StringBuilder();

        sb.append("\n")
                .append(CYAN).append(BOLD)
                .append("=====================================\n")
                .append("           HubZ Command Help\n")
                .append("=====================================\n\n")
                .append(RESET);

        // Commands
        sb.append(BOLD).append(WHITE).append("Available Commands:\n")
                .append("-------------------------------------\n").append(RESET);

        sb.append(YELLOW).append("  init                        ").append(RESET)
                .append("- Initialize a new HubZ repository (.hubz)\n");

        sb.append(YELLOW).append("  commit <message>            ").append(RESET)
                .append("- Create a new commit snapshot (no staging required)\n");

        sb.append(YELLOW).append("  log [count]                 ").append(RESET)
                .append("- Show commit history (default: 25 commits)\n");

        sb.append(YELLOW).append("  switch-root                 ").append(RESET)
                .append("- Change the base/root working directory\n");

        sb.append(YELLOW).append("  reset <hash>                ").append(RESET)
                .append("- Soft reset: update HEAD & index\n");

        sb.append(YELLOW).append("  reset --hard <hash>         ").append(RESET)
                .append("- Hard reset: update HEAD, index, and working files\n");

        sb.append(YELLOW).append("  reset --undo                ").append(RESET)
                .append("- Undo the last reset (soft or hard)\n");

        sb.append(YELLOW).append("  reset --undo --hard         ").append(RESET)
                .append("- Force undo even when working directory has changes\n");

        sb.append(YELLOW).append("  revert <hash>               ").append(RESET)
                .append("- Apply the inverse of a commit and create a new commit\n");

        sb.append(YELLOW).append("  help                        ").append(RESET)
                .append("- Show this help menu\n");

        sb.append(YELLOW).append("  exit                        ").append(RESET)
                .append("- Exit the HubZ CLI\n\n");


        // Usage Examples
        sb.append(BOLD).append(WHITE).append("Usage Examples:\n")
                .append("-------------------------------------\n").append(RESET);

        sb.append(GREEN).append("  hubz> init\n").append(RESET)
                .append("      Initialize repository metadata\n\n");

        sb.append(GREEN).append("  hubz> commit \"Initial push\"\n").append(RESET)
                .append("      Save current folder state as a commit\n\n");

        sb.append(GREEN).append("  hubz> log\n").append(RESET)
                .append("      Show recent 25 commits\n\n");

        sb.append(GREEN).append("  hubz> log 50\n").append(RESET)
                .append("      Show recent 50 commits\n\n");

        sb.append(GREEN).append("  hubz> switch-root\n").append(RESET)
                .append("      Select a new base directory\n\n");

        sb.append(GREEN).append("  hubz> reset a1b2c3d4\n").append(RESET)
                .append("      Soft reset to commit a1b2c3d4\n\n");

        sb.append(GREEN).append("  hubz> reset --hard a1b2c3d4\n").append(RESET)
                .append("      Hard reset (index + working files)\n\n");

        sb.append(GREEN).append("  hubz> reset --undo\n").append(RESET)
                .append("      Undo the previous reset\n\n");

        sb.append(GREEN).append("  hubz> reset --undo --hard\n").append(RESET)
                .append("      Force undo even with modified working directory\n\n");

        sb.append(GREEN).append("  hubz> revert a1b2c3d4\n").append(RESET)
                .append("      Create a new commit that reverses commit a1b2c3d4\n\n");


        // Notes
        sb.append(BOLD).append(WHITE).append("Notes:\n")
                .append("-------------------------------------\n").append(RESET);

        sb.append(MAGENTA)
                .append("  • The base directory must already exist — HubZ will not create it.\n")
                .append("  • 'commit' auto-detects created, deleted, and modified files.\n")
                .append("  • Each commit stores its author and branch name.\n")
                .append("  • 'log' reads the commit graph (DAG) to list history.\n")
                .append("  • 'reset' updates HEAD & index; '--hard' updates working files too.\n")
                .append("  • 'reset --undo' restores the previous HEAD/index state.\n")
                .append("  • 'reset --undo --hard' discards uncommitted changes for forced undo.\n")
                .append("  • 'revert' applies the inverse of a commit as a new commit.\n\n")
                .append(RESET);


        // Footer
        sb.append(CYAN).append(BOLD)
                .append("-------------------------------------\n")
                .append("HubZ - Lightweight Java Version Control System\n")
                .append("-------------------------------------\n")
                .append(RESET);

        return new OperationResult(true, sb.toString());
    }
}
