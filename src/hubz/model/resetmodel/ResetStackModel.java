package hubz.model.resetmodel;

import hubz.core.exception.ResetUndoNotAvailableException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ResetStackModel {

    private Deque<List<String>> resetStack = new ArrayDeque<>(); //CommitHash -> Reset Mode

    public ResetStackModel() {}

    public ResetStackModel(Deque<List<String>> stack) {
        this.resetStack = stack != null ? stack : new ArrayDeque<>();
    }

    public List<String> popResetStackElement() {
        if (resetStack == null || resetStack.isEmpty()) {
            throw new ResetUndoNotAvailableException("No reset entry available to undo.");
        }
        return resetStack.pop();
    }

    public void addResetStackElement(String commitHash, String resetMode) {
        List<String> element = new ArrayList<>(2);
        element.add(commitHash);
        element.add(resetMode);
        resetStack.push(element);
    }

    public void clear() {
        this.resetStack = new ArrayDeque<>();
    }

    public boolean isEmpty() {
        return resetStack == null || resetStack.isEmpty();
    }

    public Deque<List<String>> getResetStack() {
        return resetStack;
    }

    public void setResetStack(Deque<List<String>> resetStack) {
        this.resetStack = resetStack != null ? resetStack : new ArrayDeque<>();
    }

    //Peek last/oldest element to find first main HEAD commit hash before reset
    public synchronized String getMainHeadCommitHashForUndo() {
        if (resetStack == null || resetStack.isEmpty()) return null;
        List<String> last = resetStack.peekLast();
        return last == null ? null : last.get(0);
    }
}
