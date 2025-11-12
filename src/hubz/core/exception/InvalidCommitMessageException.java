package hubz.core.exception;

//InvalidCommitMessageException: If message is empty or null during commit operation
public class InvalidCommitMessageException extends Exception {
    public InvalidCommitMessageException(String message) {
        super(message);
    }
}
