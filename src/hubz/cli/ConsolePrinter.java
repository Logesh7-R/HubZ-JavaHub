package hubz.cli;

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
                "                   HubZ Engine\n" +
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
}
