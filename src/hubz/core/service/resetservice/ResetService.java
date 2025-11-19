package hubz.core.service.resetservice;

import hubz.context.HubzContext;
import hubz.core.exception.CommitNotFoundException;
import hubz.core.exception.RepositoryNotFoundException;
import hubz.core.service.RepositoryHelper;
import hubz.io.FileManager;
import hubz.io.JsonSerializer;
import hubz.model.OperationResult;
import hubz.model.commitmodel.CommitModel;
import hubz.model.indexmodel.IndexModel;
import hubz.model.resetmodel.ResetStackModel;
import hubz.util.HubzPath;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResetService {
    public OperationResult reset(String targetCommitHash, String resetMode) throws IOException, RepositoryNotFoundException {
        // Validate repo exists
        File rootDir = HubzContext.getRootDir();
        if (rootDir == null || !rootDir.exists()) {
            throw new RepositoryNotFoundException("Repository root not found: " + (rootDir == null ? "null" : rootDir.getAbsolutePath()));
        }

        // Traversal of graph to verify commit belongs to history
        RepositoryHelper helper = new RepositoryHelper();
        List<String> history = helper.traverseGraphFromHead();
        if (history == null || history.isEmpty() || !history.contains(targetCommitHash)) {
            return new OperationResult(false,"Commit not found in current branch history: " + targetCommitHash);
        }

        // Load current index (last committed snapshot)
        IndexModel oldIndex = helper.loadIndex();
        if (oldIndex == null) oldIndex = new IndexModel();

        // Scan working directory into newIndex
        IndexModel newIndex = new IndexModel();
        helper.scanWorkingDirectory(rootDir, newIndex);

        // If working directory is dirty (has uncommitted changes), abort
        Map<String, String> created = new LinkedHashMap<>();
        Map<String, String> modified = new LinkedHashMap<>();
        Map<String, String> deleted = new LinkedHashMap<>();

        helper.calculateWorkingDirectoryDiff(oldIndex, newIndex, created, modified, deleted);

        if (!(created.isEmpty() && modified.isEmpty() && deleted.isEmpty())) {
            return new OperationResult(false,"Working directory has uncommitted changes. Please commit before revert.");
        }

        // Load target commit and its tree
        File commitFile = new File(rootDir, HubzPath.COMMITS_DIR +File.separator+ targetCommitHash + ".json");
        if (!commitFile.exists()) {
            throw new CommitNotFoundException("Commit object missing on disk: " + commitFile.getAbsolutePath());
        }
        CommitModel targetCommit = JsonSerializer.readJsonFile(commitFile, CommitModel.class);
        if (targetCommit == null) {
            throw new CommitNotFoundException("Failed to read commit object: " + commitFile.getAbsolutePath());
        }
        targetCommit.setHash(targetCommitHash);

        IndexModel targetIndex = helper.buildTargetIndex(targetCommitHash);

        JsonSerializer.saveIndex(targetIndex);

        ResetStackModel resetStackModel = JsonSerializer.readJsonFile(new File(rootDir,
                        HubzPath.RESET_STACK_FILE), ResetStackModel.class);

        if(resetMode.equalsIgnoreCase("undo")){
            resetStackModel.popResetStackElement();
        }
        else{
            String headCommitHash = helper.getHeadCommitHash();
            resetStackModel.addResetStackElement(headCommitHash,resetMode);
        }

        JsonSerializer.saveResetStack(resetStackModel);

        File branchRefFile = new File(HubzContext.getRootDir(), helper.getBranchPath());
        FileManager.writeFile(branchRefFile.getAbsolutePath(), targetCommitHash);
        return new OperationResult(true, "Reset soft successful");
    }

    public OperationResult resetHard(String targetCommitHash, String resetMode) throws IOException, RepositoryNotFoundException {
        RepositoryHelper helper = new RepositoryHelper();
       OperationResult result = reset(targetCommitHash,resetMode);
       if(!result.isSuccess()){
           return result;
       }
        IndexModel buildingIndex = JsonSerializer.readJsonFile(HubzPath.getIndexFilePath(), IndexModel.class);
        helper.buildFolder(buildingIndex);
        return new OperationResult(true,"");
    }

    public OperationResult undoReset() throws IOException, RepositoryNotFoundException {
        //Two logic soft only update index file and hard update folder structure
        // Validate repo exists
        File rootDir = HubzContext.getRootDir();
        if (rootDir == null || !rootDir.exists()) {
            throw new RepositoryNotFoundException("Repository root not found: " + (rootDir == null ? "null" : rootDir.getAbsolutePath()));
        }

        ResetStackModel resetStackModel = JsonSerializer.readJsonFile(HubzPath.getResetStackFilePath(),ResetStackModel.class);
        List<String> resetStackElement = resetStackModel.popResetStackElement();
        String commitHash = resetStackElement.getFirst();
        String mode = resetStackElement.get(1);

        OperationResult result = new OperationResult(false,"");
        if(mode.equalsIgnoreCase("hard")){
            result = resetHard(commitHash,"undo");
        }
        else{
            result = reset(commitHash,"undo");
        }
        return result;
    }
}
