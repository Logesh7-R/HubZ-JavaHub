package hubz.cli;

import hubz.context.HubzContext;
import hubz.core.operations.*;
import hubz.io.JsonSerializer;
import hubz.model.OperationResult;
import hubz.model.RevertResult;
import hubz.model.metamodel.MetaModel;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class HubzEngine {

    public static final ConsolePrinter printer = new ConsolePrinter();
    private final Scanner scanner = new Scanner(System.in);

    //Get command, parse arguments and set context
    public void start() {
        printer.printBanner();
        printer.info("Welcome to HubZ - Java Version Control System\n");

        askForBaseDir();
        setAuthorAndBranchNameIfExist();

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

            case "switch-root":
                switchRootDirectory();
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
                handleRevertCommand(args);
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


    private void setAuthorAndBranchNameIfExist() {
        File rootDir = HubzContext.getRootDir();
        File metaFile = new File(rootDir, HubzPath.META_FILE);

        // try to load meta.json
        if (metaFile.exists()) {
            try {
                MetaModel meta = JsonSerializer.readJsonFile(metaFile, MetaModel.class);
                if (meta != null && meta.getAuthors() != null && !meta.getAuthors().isEmpty()) {
                    HubzContext.setAllAuthorsFromList(meta.getAuthors());
                }
                if(meta != null && meta.getBranch() != null && !meta.getBranch().trim().isEmpty()){
                    HubzContext.setCurrentBranchName(meta.getBranch());
                    printer.info("Current Branch: "+HubzContext.getCurrentBranchName());
                }

            } catch (IOException e) {
                printer.warn("Could not read meta.json to set author and branch name");
            }
        }

        // loop until author is set
        while (HubzContext.getAuthor() == null || HubzContext.getAuthor().trim().isEmpty())
        {
            List<String> authorNames = HubzContext.getAllAuthorsAsList();

            if (!authorNames.isEmpty()) {
                printer.info("Select option:");
                printer.info("  1) Use existing author");
                printer.info("  2) Enter new author");
                printer.prompt();
                String line = scanner.nextLine().trim();
                int mode = 0;
                try { mode = Integer.parseInt(line); } catch (NumberFormatException ignored) {}

                if (mode == 1) {
                    printer.info("Select author by id:");
                    printer.info("Id - Name");
                    for (int i = 0; i < authorNames.size(); i++) {
                        printer.info((i + 1) + " - " + authorNames.get(i));
                    }
                    printer.prompt();
                    String sel = scanner.nextLine().trim();
                    int id = -1;
                    try { id = Integer.parseInt(sel); } catch (NumberFormatException ignored) {}
                    if (id >= 1 && id <= authorNames.size()) {
                        String chosen = authorNames.get(id - 1);
                        HubzContext.setAuthor(chosen);
                        printer.info("Selected author: " + chosen);
                        break;
                    } else {
                        printer.warn("Invalid selection. Try again.");
                        continue;
                    }
                } else if (mode == 2) {

                } else {
                    printer.warn("Invalid selection. Try again.");
                    continue;
                }
            }

            // Ask for new author
            printer.info("Enter your author name (this will be stored in repository metadata):");
            printer.prompt();
            String author = scanner.nextLine().trim();
            if (author.isEmpty()) {
                printer.warn("Author name cannot be empty. Please enter again.");
                continue;
            }
            for(int i = 0; i<authorNames.size();i++){
                authorNames.set(i,authorNames.get(i).toLowerCase());
            }
            if(authorNames.contains(author.toLowerCase())){
                printer.warn("Author name is already existed. Please enter again.");
                continue;
            }
            HubzContext.setAuthor(author);
            printer.info("Author saved as: " + author);
        }
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
    private void switchRootDirectory() {
        printer.info("Enter a new base/root directory path:");
        printer.prompt();

        String newPath = scanner.nextLine().trim();

        if (newPath.isEmpty()) {
            printer.warn("Invalid path. Root directory not changed.");
            return;
        }
        try {
            HubzContext.clearContext();
            HubzContext.setRootDir(newPath);
            printer.info("Base directory successfully reset to: " +
                    HubzContext.getRootDir().getAbsolutePath());
            setAuthorAndBranchNameIfExist();
        } catch (IllegalArgumentException e) {
            printer.warn("Invalid directory: " + e.getMessage());
            printer.info("Please enter a valid existing path (absolute path recommended).");
        }
    }

    //to handle revert output
    private void handleRevertCommand(String args) {
        if (args == null || args.isEmpty()) {
            printer.warn("Please provide a commit hash.");
            printer.info("Usage: revert <commitHash>");
            return;
        }

        RevertOperation op = new RevertOperation();
        RevertResult result = op.execute(args);

        handleRevertResult(result);
    }

    private void handleRevertResult(RevertResult result) {

        switch (result.getStatus()) {

            case NOT_FOUND:
                printer.error(result.getMessage());
                break;

            case ABORTED:
                printer.warn(result.getMessage());
                break;

            case SUCCESS:
                if(!result.getChangedFiles().isEmpty()) {
                    printer.success("Revert prepared successfully for commit: " + result.getTargetCommit());
                    printer.printChangedFiles(result.getChangedFiles());
                }
                OperationResult commitResult =new CommitOperation().execute("Reverted the commit: " + result.getTargetCommit());
                if(commitResult.isSuccess()){
                    printer.info(commitResult.getMessage());
                }
                else{
                    printer.warn(commitResult.getMessage());
                }
                break;

            case CONFLICT:
                printer.warn("Revert completed with conflicts.");
                if(result.getChangedFiles().isEmpty()) {
                    printer.printConflictFiles(result.getConflictFiles());
                }
                else{
                    printer.printConflictFiles(result.getChangedFiles(),result.getConflictFiles());
                }
                printer.info("Resolve conflicts and run: commit <message>");
                break;
        }
    }
}
