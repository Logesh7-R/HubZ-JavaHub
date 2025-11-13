package hubz.model.metamodel;

import java.util.ArrayList;
import java.util.List;

public class MetaModel {
    private int commitCount = 0;
    private String branch;
    private List<String> authors = new ArrayList<>();

    public MetaModel() {}

    public MetaModel(int commitCount, String branch, List<String> authors) {
        this.commitCount = commitCount;
        this.branch = branch;
        this.authors = authors != null ? authors : new ArrayList<>();

    }

    public int getCommitCount() { return commitCount; }

    public void setCommitCount(int commitCount) { this.commitCount = commitCount; }

    public String getBranch() { return branch; }

    public void setBranch(String branch) { this.branch = branch; }

    public List<String> getAuthors() { return authors; }

    public void setAuthors(List<String> authors) { this.authors = authors; }

    public void addAuthor(String author) {
        if (author == null) return;
        if (!this.authors.contains(author)) this.authors.add(author);
    }

}
