package hubz.model;

import java.util.List;
import java.util.Map;

public class RevertResult {

    public enum Status {
        SUCCESS,
        CONFLICT,
        ABORTED,
        NOT_FOUND
    }

    private Status status;
    private String targetCommit;
    private Map<String,String> changedFiles;
    private Map<String,String> conflictFiles;
    private String message;

    public RevertResult(Status status, String targetCommit,
                        Map<String,String> changedFiles,
                        Map<String,String> conflictFiles,
                        String message) {
        this.status = status;
        this.targetCommit = targetCommit;
        this.changedFiles = changedFiles;
        this.conflictFiles = conflictFiles;
        this.message = message;
    }

    public Status getStatus() { return status; }
    public String getTargetCommit() { return targetCommit; }
    public Map<String,String> getChangedFiles() { return changedFiles; }
    public Map<String,String> getConflictFiles() { return conflictFiles; }
    public String getMessage() { return message; }
}
