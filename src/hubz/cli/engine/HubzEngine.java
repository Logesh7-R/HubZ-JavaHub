package hubz.cli.engine;

import hubz.cli.ConsolePrinter;
import hubz.core.operations.*;
import hubz.model.OperationResult;
import java.util.Scanner;

public class HubzEngine {

    public static final ConsolePrinter printer = new ConsolePrinter();
    private final Scanner scanner = new Scanner(System.in);
    private final EngineHelper engineHelper = new EngineHelper();
    //Get command, parse arguments and set context
    public void start() {
        printer.printBanner();
        printer.info("Welcome to HubZ - Java Version Control System\n");

        engineHelper.askForBaseDir();
        engineHelper.setAuthorAndBranchNameIfExist();

        while (true) {
            printer.prompt();
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                printer.info("Exiting HubZ... Goodbye!");
                break;
            }
            handleCommand(input);
        }
    }

    //Call respective operations to execute
    void handleCommand(String input) {
        if (input.isEmpty()) return;

        //Spilt into input into (command + message/args)
        String[] parts = input.trim().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = (parts.length > 1) ? parts[1].trim() : null;

        Operation operation = null;

        switch (command) {

            case "init":
                operation = new InitOperation();
                break;

            case "switch-root":
                engineHelper.switchRootDirectory();
                return;

            case "help":
                operation = new HelpOperation();
                break;
            case "reset":
                operation = new ResetOperation();
                break;

            case "commit":
                operation = new CommitOperation();
                break;

            case "log":
                operation = new LogOperation();
                break;

            case "revert":
                engineHelper.handleRevertCommand(args);
                return;

            default:
                printer.warn("Unknown command: " + command);
                printer.info("Type 'help' to see available commands");
                return;
        }

        OperationResult result = operation.execute(args);
        if (result.isSuccess())
            printer.info(result.getMessage());
        else
            printer.warn(result.getMessage());
    }
}
