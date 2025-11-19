package hubz.model.resetmodel;

import hubz.core.exception.ResetUndoNotAvailableException;

import java.util.*;

public class ResetStackModel {
    private Deque<Map<String,String>> resetStack; //Commit hash -> Reset mode
    private List<String> terminatedSnapshot; //Snapshot which become unreachable -> Path

    public ResetStackModel(){ }

     public ResetStackModel(Deque<Map<String,String>> resetStack, List<String> terminatedSnapshot) {
        this.resetStack = resetStack;
        this.terminatedSnapshot = terminatedSnapshot;
     }

     public List<String> popResetStackElement(){
        if (resetStack.isEmpty()){
            throw new ResetUndoNotAvailableException("Not have enough reset");
        }
        Map<String,String> resetStackElement = resetStack.pop();
        List<String> commitHashAndMode =new LinkedList<>();
        for(Map.Entry<String,String> entry : resetStackElement.entrySet()){
            commitHashAndMode.add(entry.getKey());
            commitHashAndMode.add(entry.getValue());
        }
        return commitHashAndMode;
    }


    public void addTerminatedSnapshot(String snapshotPath){
        terminatedSnapshot.add(snapshotPath);
    }

    public List<String>getTerminatedSnapshot(){
        return  terminatedSnapshot;
    }

    public void setTerminatedSnapshot(List<String> terminatedSnapshot){
        this.terminatedSnapshot = terminatedSnapshot;
    }

    public void addResetStackElement(Map<String,String> element){
        this.resetStack.push(element);
    }

    public Deque<Map<String,String>> getResetStack(){
        return resetStack;
    }

    public void setResetStack(Deque<Map<String,String>> resetStack){
        this.resetStack = resetStack;
    }

    public void setEmptyResetStack(){
        this.resetStack = new ArrayDeque<>();
    }

    public void setEmptyTerminatedSnapshot(){
        terminatedSnapshot = new ArrayList<>();
    }

    public String getMainHeadCommitHashForUndo(){
        Map<String,String> mainHead = resetStack.peekLast();
        String mainHeadHash = null;
        for(Map.Entry<String,String> entry : mainHead.entrySet()){
            mainHeadHash = entry.getKey();
        }
        return mainHeadHash;
    }
}
