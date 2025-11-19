package hubz.model.treemodel;

//old==null && new!=null - then it is created
//old!=null && new==null - then it is deleted
//old!=new then it is modified
public class TreeEntry {
    private String oldBlob;
    private String newBlob;
    private long size;     // file size in bytes
    private long mtime;    // last modified time

    public TreeEntry(){}

    public TreeEntry(String oldBlobHash, String newBlobHash){
        this.oldBlob = oldBlobHash;
        this.newBlob = newBlobHash;
    }

    public TreeEntry(String oldBlobHash, String newBlobHash,long size, long mtime){
        this.oldBlob = oldBlobHash;
        this.newBlob = newBlobHash;
        this.size = size;
        this.mtime = mtime;
    }

    public String getOldBlob(){
        return oldBlob;
    }

    public void setOldBlob(String oldBlob){
        this.oldBlob = oldBlob;
    }

    public String getModifiedBlob(){
        return newBlob;
    }

    public void setNewBlob(String newBlob){
        this.newBlob = newBlob;
    }

    public void setCreatedBlob(String createdBlob){
        this.newBlob = createdBlob;
        this.oldBlob = null;
    }

    public void setDeletedBlob(String deletedBlob){
        this.oldBlob = deletedBlob;
        this.newBlob = null;
    }

    public String getCreatedBlob(){
        return this.newBlob;
    }

    public String getDeletedBlob(){
        return this.oldBlob;
    }

    public boolean isCreated() {
        return oldBlob == null && newBlob != null;
    }

    public boolean isDeleted() {
        return oldBlob != null && newBlob == null;
    }

    public boolean isModified() {
        return oldBlob != null && newBlob != null && !oldBlob.equals(newBlob);
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
