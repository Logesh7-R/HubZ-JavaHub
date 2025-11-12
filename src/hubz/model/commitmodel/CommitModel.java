package hubz.model.commitmodel;

import hubz.context.HubzContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommitModel {
    transient private String hash; // Commit hash
    private String parent; //Parent commit hash
    private String tree; //Tree hash
    private String message; //Commit message
    private String author = HubzContext.getAuthor(); //Author name
    private String timestamp;//Timestamp yyyy-mm-dd't'hh-mm-ss
    private int commitNumber; //Current commit count
    private Map<String, String> deletedFiles = new LinkedHashMap<>(); //Relative path and absolute path of deleted file

    public CommitModel() {}

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getCommitNumber() {
        return commitNumber;
    }

    public void setCommitNumber(int commitNumber) {
        this.commitNumber = commitNumber;
    }

    public Map<String, String> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(Map<String, String> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }
}
