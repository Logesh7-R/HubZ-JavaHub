package hubz.core.printer;

public class ConsolePrinter {

    public void printBanner() {
        System.out.println("============================");
        System.out.println("            HubZ Engine");
        System.out.println("============================");
    }

    public void info(String message) {
        System.out.println(message);
    }

    public void warn(String message) {
        System.out.println("[Warning] " + message);
    }

    public void error(String message) {
        System.out.println("[Error] " + message);
    }

    public void prompt() {
        System.out.print("hubz> ");
    }
}


