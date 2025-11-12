package hubz.core.operations;

import hubz.context.HubzContext;
import hubz.core.service.initservice.RepoInitializer;
import hubz.model.OperationResult;
import hubz.core.exception.RepositoryInitException;

import java.io.File;

//Execute init operation
public class InitOperation implements Operation {

    @Override
    public OperationResult execute(String arg) {
        File rootDir = HubzContext.getRootDir();

        //Commiting before setting root directory
        if (rootDir == null) {
            return new OperationResult(false, "Root directory not set. Please restart HubZ.");
        }

        //Repository aldready initialized
        File hubzDir = new File(rootDir, ".hubz");
        if (hubzDir.exists()) {
            return new OperationResult(false,
                    "Repository already initialized: " + hubzDir.getAbsolutePath());
        }

        //Using RepoInitializer service to operate
        RepoInitializer repoInitializer = new RepoInitializer();
        try {
            repoInitializer.initializeRepoStructure(rootDir);
            return new OperationResult(true,
                    "Initialized HubZ repository in: " + hubzDir.getAbsolutePath());
        } catch (RepositoryInitException e) {
            return new OperationResult(false, e.getMessage());
        }
    }
}
