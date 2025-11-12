package hubz.core.operations;

import hubz.model.OperationResult;

//Execute help operation
public class HelpOperation implements Operation {

    @Override
    public OperationResult execute(String arg) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n=====================================\n");
        sb.append("           HubZ Command Help\n");
        sb.append("=====================================\n\n");

        sb.append("Available Commands:\n");
        sb.append("-------------------------------------\n");
        sb.append("  init      - Initialize a new HubZ repository (.hubz folder) in the base directory\n");
        sb.append("  commit    - Create a new commit snapshot directly (no staging required)\n");
        sb.append("  reset     - Change or reset the current base/root directory\n");
        sb.append("  help      - Show this help menu with available commands\n");
        sb.append("  exit      - Exit the HubZ CLI\n\n");

        sb.append("Usage Examples:\n");
        sb.append("-------------------------------------\n");
        sb.append("  hubz> init                # Create a .hubz folder inside your current base directory\n");
        sb.append("  hubz> commit Initial push # Save current folder state as a commit\n");
        sb.append("  hubz> reset               # Choose a new root/base directory\n");
        sb.append("  hubz> help                # Display this help list\n");
        sb.append("  hubz> exit                # Exit from HubZ CLI\n\n");

        sb.append("Notes:\n");
        sb.append("-------------------------------------\n");
        sb.append("  • The base directory must already exist — HubZ will not create it.\n");
        sb.append("  • 'init' sets up a .hubz folder to start version tracking.\n");
        sb.append("  • 'commit' saves all current files directly without staging.\n");
        sb.append("  • 'reset' allows you to change the working base directory anytime.\n");
        sb.append("  • Always ensure you are inside a valid HubZ repository before committing.\n\n");

        sb.append("-------------------------------------\n");
        sb.append("HubZ - Lightweight Java Version Control System\n");
        sb.append("-------------------------------------\n");

        return new OperationResult(true, sb.toString());
    }
}
