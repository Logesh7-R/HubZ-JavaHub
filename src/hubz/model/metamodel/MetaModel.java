package hubz.model.metamodel;

public class MetaModel {
    private int commitCount;
    private String branch;
    private String author;

    public MetaModel() {}

    public MetaModel(int commitCount, String branch,String author) {
        this.commitCount = commitCount; //Commit count so far
        this.branch = branch; //Current Working branch
        this.author = author; // Current working author name
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
