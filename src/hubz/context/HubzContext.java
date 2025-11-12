package hubz.context;

import java.io.File;

//Global context storing Root directory, author name
public class HubzContext {

    private static File rootDir;
    private static boolean isInitialized = false;
    private static String author;
    public static void setRootDir(String path) throws IllegalArgumentException {
        File dir = new File(path.trim());

        if (!dir.isAbsolute()) {
            dir = dir.getAbsoluteFile();
        }

        if (!dir.exists()) {
            throw new IllegalArgumentException("Directory does not exist: " + dir.getAbsolutePath());
        }

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The given path points to a file, not a directory: " + dir.getAbsolutePath());
        }
        isInitialized = true;
        rootDir = dir;
    }

    public static File getRootDir() {
        return rootDir;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setAuthor(String name) {
            author = name.trim();
    }

    public static String getAuthor() {
        return author;
    }
}

