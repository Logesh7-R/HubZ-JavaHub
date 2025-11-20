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
import java.util.*;


// Reset commits
// Three modes: normal, hard, undo
public class ResetService {

    private final RepositoryHelper helper = new RepositoryHelper();


    // Normal reset will reset only index.json and did not touch folder structure
    public OperationResult reset(String targetCommitHash, String resetMode)
            throws IOException, RepositoryNotFoundException {

        // Validate repo root
        File rootDir = HubzContext.getRootDir();
        if (rootDir == null || !rootDir.exists()) {
            throw new RepositoryNotFoundException("Repository root not found.");
        }

        // Load undo stack
        File resetStackFile = HubzPath.getResetStackFilePath();
        ResetStackModel resetStack = null;
        Deque<List<String>> undoStack = null;

        if (resetStackFile.exists()) {
            resetStack = JsonSerializer.readJsonFile(resetStackFile, ResetStackModel.class);
            undoStack = (resetStack != null) ? resetStack.getResetStack() : null;
        }

        // Validate target commit
        List<String> history = helper.traverseGraphFromHead();
        boolean targetInHistory = history != null && history.contains(targetCommitHash);
        boolean targetInUndoStack = false;

        // If not found in history, only valid during undo or undo-hard
        if (!targetInHistory) {
            if (!"undo".equalsIgnoreCase(resetMode) && !"undo-hard".equalsIgnoreCase(resetMode)) {
                return new OperationResult(false,
                        "Commit not found in current branch history: " + targetCommitHash);
            }

            if (undoStack != null) {
                for (List<String> entry : undoStack) {
                    if (targetCommitHash.equals(entry.get(0))) {
                        targetInUndoStack = true;
                        break;
                    }
                }
            }

            if (!targetInUndoStack) {
                return new OperationResult(false,
                        "Undo failed: commit not present in undo stack: " + targetCommitHash);
            }
        }

        // Load HEAD — if HEAD == target, then do nothing
        File branchRefFile = new File(rootDir,
                HubzPath.HUBZ_DIR + File.separator + helper.getBranchPath());

        String headHash = FileManager.readFile(branchRefFile.getAbsolutePath()).trim();
        if (headHash != null && headHash.equals(targetCommitHash)) {
            return new OperationResult(true,
                    "Already at commit " + targetCommitHash + ". Nothing to reset.");
        }

        // Load current index.json
        IndexModel lastIndex = helper.loadIndex();
        if (lastIndex == null) lastIndex = new IndexModel();

        // Scan working directory
        IndexModel workingIndex = new IndexModel();
        helper.scanWorkingDirectory(rootDir, workingIndex);

        Map<String, String> created = new LinkedHashMap<>();
        Map<String, String> modified = new LinkedHashMap<>();
        Map<String, String> deleted = new LinkedHashMap<>();
        helper.calculateWorkingDirectoryDiff(lastIndex, workingIndex, created, modified, deleted);

        // Baseline rebuild variables
        String baselineCommitHash = null;
        String baselineMode = null;
        IndexModel baselineIndex = null;

        //If working directory is dirty, then check with reset --hard folder structure in reset stack
        //Skip, if it is undo-hard
        if (!"undo-hard".equalsIgnoreCase(resetMode)) {

            boolean workingDirty = !(created.isEmpty() && modified.isEmpty() && deleted.isEmpty());

            if (workingDirty) {
                //If there is no reset stack, then it is first reset to enter, then ask user to save changes
                if (undoStack == null || undoStack.isEmpty()) {
                    return new OperationResult(false,
                            "Working directory has uncommitted changes. Commit or undo previous reset before resetting again.");
                }

                //Start to build reset stack index of hard reset commit hash
                List<List<String>> stackList = new ArrayList<>(undoStack);

                // Try to find last HARD reset
                for(int i=0; i< stackList.size();i++){
                    List<String> entry = stackList.get(i);
                    if("hard".equalsIgnoreCase(entry.get(1)) && i!=0){
                        //i-1 because in reset stack it will store previous head but,
                        // we need commit hash which done reset --hard <needed hash>
                        entry = stackList.get(i-1);
                        baselineCommitHash = entry.get(0);
                        baselineMode = entry.get(1);
                        break;
                    }
                }

                // No HARD reset → pick first SOFT entry, if SOFT reset presents in reset stack
                if (baselineCommitHash == null) {
                    List<String> firstSoft = stackList.get(stackList.size() - 1);
                    if(firstSoft.get(1)!=null && !firstSoft.get(1).trim().isEmpty() && "soft".equalsIgnoreCase(firstSoft.get(1))) {
                        baselineCommitHash = firstSoft.get(0);
                        baselineMode = firstSoft.get(1);
                    }
                }

                boolean stillDirty;
                // Rebuild the index of reset hard commit
                if(baselineCommitHash!=null) {
                    baselineIndex = helper.rebuildIndexAtCommit(baselineCommitHash);


                    Map<String, String> uCreated = new LinkedHashMap<>();
                    Map<String, String> uModified = new LinkedHashMap<>();
                    Map<String, String> uDeleted = new LinkedHashMap<>();

                    helper.calculateWorkingDirectoryDiff(
                            baselineIndex, workingIndex,
                            uCreated, uModified, uDeleted
                    );

                     stillDirty =
                            !(uCreated.isEmpty() && uModified.isEmpty() && uDeleted.isEmpty());
                }
                else{
                    stillDirty = true;
                }
                if (stillDirty) {
                    return new OperationResult(false,
                            "Conflicting changes detected from a previous reset (" + baselineMode + "). " +
                                    "Commit your modifications or execute reset --undo --hard to discard them.");
                }
            }
        }

        // Validate commit object exists
        File commitFile = HubzPath.getCommitFilePath(targetCommitHash);
        if (!commitFile.exists()) {
            throw new CommitNotFoundException(
                    "Commit object missing: " + commitFile.getAbsolutePath());
        }

        CommitModel targetCommit = JsonSerializer.readJsonFile(commitFile, CommitModel.class);
        if (targetCommit == null) {
            throw new CommitNotFoundException(
                    "Failed to read commit object: " + targetCommitHash);
        }

        // Build final index
        IndexModel targetIndex;
        if (baselineCommitHash != null && baselineCommitHash.equals(targetCommitHash)) {
            targetIndex = baselineIndex;
        } else {
            targetIndex = helper.rebuildIndexAtCommit(targetCommitHash);
        }

        if (targetIndex == null) {
            return new OperationResult(false,
                    "Failed to build index for target commit: " + targetCommitHash);
        }

        // Initialize reset stack if needed
        if (resetStack == null) resetStack = new ResetStackModel();

        // Add current HEAD for soft & hard reset
        if ("soft".equalsIgnoreCase(resetMode) || "hard".equalsIgnoreCase(resetMode)) {
            String currentHead = helper.getHeadCommitHash();
            resetStack.addResetStackElement(
                    currentHead == null ? "" : currentHead,
                    resetMode
            );
        }

        // HARD and UNDO-HARD resets override previous mode to hard
        if ("hard".equalsIgnoreCase(resetMode) || "undo-hard".equalsIgnoreCase(resetMode)) {
            Deque<List<String>> stack = resetStack.getResetStack();
            for (List<String> entry : stack) entry.set(1, "hard");
            resetStack.setResetStack(stack);
        }

        // Persist stack only for reset (not undo)
        if (!"undo".equalsIgnoreCase(resetMode) &&
                !"undo-hard".equalsIgnoreCase(resetMode)) {
            JsonSerializer.saveResetStack(resetStack);
        }

        // Persist computed index
        JsonSerializer.saveIndex(targetIndex);

        // Update branch ref
        FileManager.writeFile(branchRefFile.getAbsolutePath(), targetCommitHash);

        return new OperationResult(true,
                "Reset (soft) successful. HEAD -> " + targetCommitHash);
    }


    // reset --hard: modify both index json and folder structure
    public OperationResult resetHard(String targetCommitHash, String resetMode)
            throws IOException, RepositoryNotFoundException {

        OperationResult softResult = reset(targetCommitHash, resetMode);
        if (!softResult.isSuccess()) return softResult;

        IndexModel buildingIndex =
                JsonSerializer.readJsonFile(HubzPath.getIndexFilePath(), IndexModel.class);

        if (buildingIndex == null) {
            return new OperationResult(false,
                    "Failed to read newly built index for hard reset.");
        }

        helper.applyIndexToWorkingDirectory(buildingIndex);

        return new OperationResult(true,
                "Reset (hard) successful. Worktree updated. HEAD -> " + targetCommitHash);
    }


    // Undo reset operation (LIFO)
    public OperationResult undoReset(boolean isUndoHard)
            throws IOException, RepositoryNotFoundException {

        File resetStackFile = HubzPath.getResetStackFilePath();

        if (!resetStackFile.exists()) {
            return new OperationResult(false, "No reset history available to undo.");
        }

        ResetStackModel resetStack =
                JsonSerializer.readJsonFile(resetStackFile, ResetStackModel.class);

        if (resetStack == null || resetStack.getResetStack() == null ||
                resetStack.getResetStack().isEmpty()) {
            return new OperationResult(false, "No reset history available to undo.");
        }

        // Pop top entry
        List<String> entry = resetStack.popResetStackElement();
        if (entry == null || entry.size() < 2) {
            return new OperationResult(false,
                    "Corrupted reset history. Cannot undo.");
        }

        String previousHead = entry.get(0);
        String stackMode = entry.get(1);

        if (previousHead == null || previousHead.trim().isEmpty()) {
            return new OperationResult(false,
                    "Stored previous commit is invalid. Cannot undo reset.");
        }

        OperationResult result;

        // Option A Undo behavior
        if ("soft".equalsIgnoreCase(stackMode)) {
            result = isUndoHard
                    ? resetHard(previousHead, "undo-hard")
                    : reset(previousHead, "undo");
        } else {
            result = isUndoHard
                    ? resetHard(previousHead, "undo-hard")
                    : resetHard(previousHead, "undo");
        }

        if (result.isSuccess()) {
            JsonSerializer.saveResetStack(resetStack);
            return new OperationResult(true,
                    "Undo successful.\nHEAD restored to: " + previousHead);
        }

        return new OperationResult(false,
                "Undo failed: " + result.getMessage());
    }
}
