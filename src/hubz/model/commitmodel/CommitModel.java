package hubz.model.commitmodel;

import hubz.context.HubzContext;

public class CommitModel {
    transient private String hash; // Commit hash
    private String parent; //Parent commit hash
    private String treeHash; //Tree hash
    private String message; //Commit message
    private String author = HubzContext.getAuthor(); //Author name
    private String timestamp;//Timestamp yyyy-mm-dd't'hh-mm-ss
    private String branchName;//Current branch name
    private int commitNumber; //Current commit count


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

    public String getTreeHash() {
        return treeHash;
    }

    public void setTreeHash(String treeHash) {
        this.treeHash = treeHash;
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

    public void setBranchName(String branchName){ this.branchName = branchName; }

    public String getBranchName(){ return branchName; }
}
