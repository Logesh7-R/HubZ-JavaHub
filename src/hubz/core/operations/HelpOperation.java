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

        sb.append(YELLOW).append("  init                      ").append(RESET)
                .append("- Initialize a new HubZ repository (.hubz folder)\n");

        sb.append(YELLOW).append("  commit <message>          ").append(RESET)
                .append("- Create a new commit snapshot (no staging required)\n");

        sb.append(YELLOW).append("  log [count]               ").append(RESET)
                .append("- Show commit history (default: 25 commits)\n");

        sb.append(YELLOW).append("  switch-root               ").append(RESET)
                .append("- Change or reset the base directory\n");

        sb.append(YELLOW).append("  reset <hash>              ").append(RESET)
                .append("- Reset HEAD & index to a target commit (soft reset)\n");

        sb.append(YELLOW).append("  reset --hard <hash>       ").append(RESET)
                .append("- Reset HEAD + index + working directory\n");

        sb.append(YELLOW).append("  reset --undo              ").append(RESET)
                .append("- Undo the last reset (soft or hard)\n");

        sb.append(YELLOW).append("  revert <hash>             ").append(RESET)
                .append("- Revert a specific commit by creating a new inverse commit\n");

        sb.append(YELLOW).append("  help                      ").append(RESET)
                .append("- Show this help menu\n");

        sb.append(YELLOW).append("  exit                      ").append(RESET)
                .append("- Exit the HubZ CLI\n\n");


        // Usage Examples
        sb.append(BOLD).append(WHITE).append("Usage Examples:\n")
                .append("-------------------------------------\n").append(RESET);

        sb.append(GREEN).append("  hubz> init\n").append(RESET)
                .append("      Create a .hubz folder in the base directory\n\n");

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
                .append("      Hard reset (index + files)\n\n");

        sb.append(GREEN).append("  hubz> reset --undo\n").append(RESET)
                .append("      Undo last reset\n\n");

        sb.append(GREEN).append("  hubz> revert a1b2c3d4\n").append(RESET)
                .append("      Create a new commit that undoes commit a1b2c3d4\n\n");


        // Notes
        sb.append(BOLD).append(WHITE).append("Notes:\n")
                .append("-------------------------------------\n").append(RESET);

        sb.append(MAGENTA)
                .append("  • The base directory must already exist — HubZ will not create it.\n")
                .append("  • 'init' creates the internal .hubz folder.\n")
                .append("  • 'commit' auto-detects created, modified and deleted files.\n")
                .append("  • HubZ supports multiple authors; you can select your author at startup.\n")
                .append("  • Current branch name is tracked in each commit.\n")
                .append("  • 'log' retrieves commit history from the commit graph.\n")
                .append("  • 'reset' updates HEAD and index; 'reset --hard' updates working files too.\n")
                .append("  • 'reset --undo' restores the previous HEAD and index state.\n")
                .append("  • 'revert' generates a new commit that inverses changes of a given commit.\n\n")
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
