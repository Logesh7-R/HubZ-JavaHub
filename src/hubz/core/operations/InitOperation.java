package hubz.core.operations;

import hubz.core.HubZContext;
import hubz.core.io.FileManager;
import hubz.core.model.OperationResult;
import hubz.core.exception.RepositoryInitException;

import java.io.File;

public class InitOperation implements Operation {

    @Override
    public OperationResult execute(String arg) {
        File rootDir = HubZContext.getRootDir();

        if (rootDir == null) {
            return new OperationResult(false, "Root directory not set. Please restart HubZ.");
        }

        File hubzDir = new File(rootDir, ".hubz");
        if (hubzDir.exists()) {
            return new OperationResult(false,
                    "Repository already initialized: " + hubzDir.getAbsolutePath());
        }

        FileManager fm = new FileManager();
        try {
            fm.initializeRepoStructure(rootDir);
            return new OperationResult(true,
                    "Initialized HubZ repository in: " + hubzDir.getAbsolutePath());
        } catch (RepositoryInitException e) {
            return new OperationResult(false, e.getMessage());
        }
    }
}
