package hubz.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

public class FileManager {

    private FileManager() {}

    public static void createDir(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dirPath);
        }
    }

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

    public static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static void writeFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    public static void atomicWrite(File target, String content) throws IOException {
        File parentDir = target.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories for: " + target.getAbsolutePath());
            }
        }

        File tempFile = new File(parentDir != null ? parentDir : new File("."), target.getName() + ".tmp");

        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException("Failed to delete existing temp file: " + tempFile.getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
            writer.flush();
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
}

