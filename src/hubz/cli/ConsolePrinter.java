package hubz.cli;

import java.util.Map;

public class ConsolePrinter {

    // ANSI Escape Codes
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";

    private static final String RED     = "\u001B[31m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String BLUE    = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN    = "\u001B[36m";
    private static final String WHITE   = "\u001B[37m";

    public void printBanner() {
        System.out.println(CYAN + BOLD +
                "=====================================\n" +
                "              HubZ Engine\n" +
                "=====================================" +
                RESET);
    }

    public void info(String message) {
        System.out.println(WHITE + message + RESET);
    }

    public void success(String message) {
        System.out.println(GREEN + message + RESET);
    }

    public void warn(String message) {
        System.out.println(YELLOW + "[Warning] " + message + RESET);
    }

    public void error(String message) {
        System.out.println(RED + "[Error] " + message + RESET);
    }

    public void prompt() {
        System.out.print(CYAN + BOLD + "hubz> " + RESET);
    }


    public void printChangedFiles(Map<String,String> files) {
        section("Changed Files");
        if (files == null || files.isEmpty()) {
            info("No changed files.");
            return;
        }
        System.out.println(BLUE + BOLD + "Changed Files:" + RESET);
        for (Map.Entry<String,String> file : files.entrySet()) {
            String relativePath = file.getKey();
            String changes = file.getValue();
            System.out.println(BLUE + "  ● " +relativePath+" - "+changes + RESET);
        }
    }

    public void printConflictFiles(Map<String,String> conflictFiles) {
        section("Conflicting Files");
        if (conflictFiles == null || conflictFiles.isEmpty()) {
            info("No conflicts.");
            return;
        }

        System.out.println(RED + BOLD + "Conflicts Found:" + RESET);

        for (Map.Entry<String,String> file : conflictFiles.entrySet()) {
            String relativePath = file.getKey();
            String changes = file.getValue();
            System.out.println(RED + BOLD + "  ✖ " +relativePath+" - "+changes + RESET);
        }

        System.out.println(YELLOW +
                "Resolve the conflict markers (<<<<<<<, =======, >>>>>>>) inside the files.\n" +
                "Then run: commit <message>" +
                RESET);
    }
    public void printConflictFiles(Map<String,String> changedFiles,Map<String,String> conflictFiles){
        printChangedFiles(changedFiles);
        printConflictFiles(conflictFiles);
    }

    public void section(String title) {
        System.out.println(MAGENTA + BOLD + "\n=== " + title + " ===" + RESET);
    }

}
