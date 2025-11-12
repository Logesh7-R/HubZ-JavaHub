package hubz.core.model.indexmodel;

public class IndexEntry {

    private String hash;   // blob hash
    private long size;     // file size in bytes
    private long mtime;    // last modified time

    public IndexEntry() {}

    public IndexEntry(String hash, long size, long mtime) {
        this.hash = hash;
        this.size = size;
        this.mtime = mtime;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }
}
