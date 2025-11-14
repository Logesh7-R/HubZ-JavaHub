package hubz.context;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

//Global context storing Root directory, author name
public class HubzContext {

    private HubzContext() {}

    private static File rootDir;
    private static boolean initialized = false;
    private static String currentBranchName = "main";
    private static String currentAuthor;

    private static LinkedHashSet<String> authors = new LinkedHashSet<>();

    public static void setRootDir(String path) throws IllegalArgumentException {
        File dir = new File(path.trim());
        if (!dir.isAbsolute()) dir = dir.getAbsoluteFile();

        if (!dir.exists()) {
            throw new IllegalArgumentException("Directory does not exist: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The given path points to a file, not a directory: " + dir.getAbsolutePath());
        }

        rootDir = dir;
        initialized = true;
    }

    public static File getRootDir() { return rootDir; }

    public static boolean isInitialized() { return initialized; }

    public static void setAuthor(String name) {
        if (name == null) return;
        currentAuthor = name.trim();
        if (!currentAuthor.isEmpty()) authors.add(currentAuthor);
    }

    public static String getAuthor() { return currentAuthor; }

    public static void addAuthor(String author) {
        if (author == null) return;
        String t = author.trim();
        if (!t.isEmpty()) authors.add(t);
    }

    public static List<String> getAllAuthorsAsList() {
        return new ArrayList<>(authors);
    }

    public static void setAllAuthorsFromList(List<String> list) {
        if (list != null) {
            authors.clear();
            for (String s : list) {
                if (s != null && !s.trim().isEmpty()) authors.add(s.trim());
            }
        }
    }

    public static String getCurrentBranchName() { return currentBranchName; }

    public static void setCurrentBranchName(String branchName) { currentBranchName = branchName; }

    public static void clearContext(){
        rootDir = null ;
        initialized = false;
        currentBranchName = "main";
        currentAuthor = null;
    }
}
