package hubz.model.resetmodel;

import hubz.core.exception.ResetUndoNotAvailableException;

import java.util.*;

public class ResetStackModel {
    private Deque<List<String>> resetStack; //Commit hash -> Reset mode

    public ResetStackModel(){ }

     public ResetStackModel(Deque<List<String>> resetStack) {
        this.resetStack = resetStack;
     }

     public List<String> popResetStackElement(){
        if (resetStack.isEmpty()){
            throw new ResetUndoNotAvailableException("Not have enough reset");
        }
        return resetStack.pop();
    }

    public void addResetStackElement(String commitHash, String resetMode){
        List<String> element = new ArrayList<>();
        element.add(commitHash);
        element.add(resetMode);
        this.resetStack.push(element);
    }

    public Deque<List<String>> getResetStack(){
        return resetStack;
    }

    public void setResetStack(Deque<List<String>> resetStack){
        this.resetStack = resetStack;
    }

    public void setEmptyResetStack(){
        this.resetStack = new ArrayDeque<>();
    }

    public String getMainHeadCommitHashForUndo(){
        List<String> mainHead = resetStack.peekLast();
        if(mainHead==null){
            return null;
        }
        return mainHead.getFirst();
    }
}
