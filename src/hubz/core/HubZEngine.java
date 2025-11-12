package hubz.core;

import hubz.core.io.Serializer;
import hubz.core.model.OperationResult;
import hubz.core.model.metamodel.MetaModel;
import hubz.core.operations.*;
import hubz.core.printer.ConsolePrinter;
import hubz.core.util.HubZPath;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class HubZEngine {

    public static final ConsolePrinter printer = new ConsolePrinter();
    private final Scanner scanner = new Scanner(System.in);

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

    private void handleCommand(String input) {
        if (input.isEmpty()) return;

        String[] parts = input.trim().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = (parts.length > 1) ? parts[1].trim() : null;

        Operation operation = null;

        switch (command) {

            case "init":
                operation = new InitOperation();
                break;

            case "reset":
                resetBaseDirectory();
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

    private void setAuthorNameIfExist(){
        File rootDir = HubZContext.getRootDir();
        File metaFile = new File(rootDir, HubZPath.META_FILE);
        try {
            if (metaFile.exists()) {
                MetaModel meta = Serializer.readJsonFile(metaFile, MetaModel.class);
                if(meta != null && meta.getAuthor() != null && !meta.getAuthor().trim().isEmpty()){
                    HubZContext.setAuthor(meta.getAuthor());
                    printer.info("Author loaded from repository: " + meta.getAuthor());
                    return;
                }
            }
        } catch (IOException e) {
           printer.warn("Could not read meta.json to set author name");
        }

        printer.info("Enter your author name (this will be stored in repository metadata):");
        printer.prompt();
        String author = scanner.nextLine().trim();

        while(author.isEmpty()){
            printer.warn("Author name cannot be empty. Please enter again:");
            printer.prompt();
            author = scanner.nextLine().trim();
        }

        HubZContext.setAuthor(author);
        printer.info("Author saved as: " + author);
    }

    private void askForBaseDir() {
        while (!HubZContext.isInitialized()) {
            printer.info("Please enter your project root directory (example: D:\\Projects\\MyApp):");
            printer.prompt();
            String path = scanner.nextLine().trim();

            if (path.isEmpty()) {
                printer.warn("Path cannot be empty. Please try again.");
                continue;
            }

            try {
                HubZContext.setRootDir(path);
                printer.info("Base directory set to: " + HubZContext.getRootDir().getAbsolutePath());
            } catch (IllegalArgumentException e) {
                printer.warn("Invalid directory: " + e.getMessage());
                printer.info("Please enter a valid existing path (absolute path recommended).");
            }
        }
    }

    private void resetBaseDirectory() {
        printer.info("Enter a new base/root directory path:");
        printer.prompt();

        String newPath = scanner.nextLine().trim();

        if (newPath.isEmpty()) {
            printer.warn("Invalid path. Root directory not changed.");
            return;
        }
        try {
            HubZContext.setRootDir(newPath);
            printer.info("Base directory successfully reset to: " +
                    HubZContext.getRootDir().getAbsolutePath());
        } catch (IllegalArgumentException e) {
            printer.warn("Invalid directory: " + e.getMessage());
            printer.info("Please enter a valid existing path (absolute path recommended).");
        }
    }
}
