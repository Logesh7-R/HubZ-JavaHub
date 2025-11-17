package hubz.io;

import hubz.context.HubzContext;
import hubz.util.HubzPath;

import java.io.*;
import java.nio.file.*;

//Handle file and directory CRUD operations
public class FileManager {

    private FileManager() {}

    //Creating directory
    public static void createDir(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dirPath);
        }
    }

    //Create file with specified content
    public static void createFile(String filePath, String content) throws IOException {
        File file = new File(filePath);

        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Failed to create file: " + filePath);
        }

        if (content != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
        }
    }

    //Buffer reader for files
    public static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath)))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        }

        return content.toString();
    }

    public static void deleteFileAndCleanParents(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.exists()) {
            if (!file.isFile()) {
                throw new IOException("Cannot delete: not a regular file → " + file.getAbsolutePath());
            }
            if (!file.delete()) {
                throw new IOException("Failed to delete file: " + file.getAbsolutePath());
            }
        } else {
            return;
        }

        //Cleanup folder, if folder is empty
        File parent = file.getParentFile();
        File rootDir = HubzContext.getRootDir();
        while (parent != null && !parent.equals(rootDir)) {
            File[] children = parent.listFiles();

            if (children != null && children.length == 0) {
                if (!parent.delete()) {
                    throw new IOException("Failed to delete empty directory: " + parent.getAbsolutePath());
                }
                parent = parent.getParentFile();
            } else {
                break; // stop if directory not empty
            }
        }
    }



    public static void deleteFileAndCleanParents(String path) throws IOException {
        deleteFileAndCleanParents(new File(path));
    }

    public static void writeFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    //Atomic write will perform using temp folder
    public static void atomicWrite(File target, String content) throws IOException {
        File tempDir = new File(HubzContext.getRootDir(), HubzPath.TEMP_DIR);

        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IOException("Failed to create temp directory: " + tempDir.getAbsolutePath());
        }

        File tempFile = new File(tempDir, target.getName() + ".tmp");

        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException("Failed to delete existing temp file: " + tempFile.getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        try {
            Files.move(
                    tempFile.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public static void atomicWrite(String target, String content) throws IOException{
        atomicWrite(new File(target),content);
    }
}

