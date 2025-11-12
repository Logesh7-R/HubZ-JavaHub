package hubz.cli;

import hubz.context.HubzContext;
import hubz.io.JsonSerializer;
import hubz.model.OperationResult;
import hubz.model.metamodel.MetaModel;
import hubz.core.operations.CommitOperation;
import hubz.core.operations.HelpOperation;
import hubz.core.operations.InitOperation;
import hubz.core.operations.Operation;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class HubzEngine {

    public static final ConsolePrinter printer = new ConsolePrinter();
    private final Scanner scanner = new Scanner(System.in);

    //Get command, parse arguments and set context
    public void start() {
        printer.printBanner();
        printer.info("Welcome to HubZ - Java Version Control System\n");

        askForBaseDir();
        setAuthorNameIfExist();

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
    private void handleCommand(String input) {
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

            case "reset":
                resetRootDirectory();
                return;

            case "help":
                operation = new HelpOperation();
                break;

            case "commit":
                operation = new CommitOperation();
                break;

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

    // Set author name at context
    private void setAuthorNameIfExist(){
        File rootDir = HubzContext.getRootDir();
        File metaFile = new File(rootDir, HubzPath.META_FILE);
        try {
            //Load author name from meta file if exist
            if (metaFile.exists()) {
                MetaModel meta = JsonSerializer.readJsonFile(metaFile, MetaModel.class);
                if(meta != null && meta.getAuthor() != null && !meta.getAuthor().trim().isEmpty()){
                    HubzContext.setAuthor(meta.getAuthor());
                    printer.info("Author loaded from repository: " + meta.getAuthor());
                    return;
                }
            }
        } catch (IOException e) {
           printer.warn("Could not read meta.json to set author name");
        }

        //If meta file did not exist, then author name from user and set it in context
        printer.info("Enter your author name (this will be stored in repository metadata):");
        printer.prompt();
        String author = scanner.nextLine().trim();

        while(author.isEmpty()){
            printer.warn("Author name cannot be empty. Please enter again:");
            printer.prompt();
            author = scanner.nextLine().trim();
        }

        HubzContext.setAuthor(author);
        printer.info("Author saved as: " + author);
    }

    //Set root directory at context, if path did not exist ask user to create it
    private void askForBaseDir() {
        while (!HubzContext.isInitialized()) {
            printer.info("Please enter your project root directory (example: D:\\Projects\\MyApp):");
            printer.prompt();
            String path = scanner.nextLine().trim();

            if (path.isEmpty()) {
                printer.warn("Path cannot be empty. Please try again.");
                continue;
            }

            try {
                HubzContext.setRootDir(path);
                printer.info("Base directory set to: " + HubzContext.getRootDir().getAbsolutePath());
            } catch (IllegalArgumentException e) {
                printer.warn("Invalid directory: " + e.getMessage());
                printer.info("Please enter a valid existing path (absolute path recommended).");
            }
        }
    }

    //Reset root directory at context
    private void resetRootDirectory() {
        printer.info("Enter a new base/root directory path:");
        printer.prompt();

        String newPath = scanner.nextLine().trim();

        if (newPath.isEmpty()) {
            printer.warn("Invalid path. Root directory not changed.");
            return;
        }
        try {
            HubzContext.setRootDir(newPath);
            printer.info("Base directory successfully reset to: " +
                    HubzContext.getRootDir().getAbsolutePath());
        } catch (IllegalArgumentException e) {
            printer.warn("Invalid directory: " + e.getMessage());
            printer.info("Please enter a valid existing path (absolute path recommended).");
        }
    }
}
