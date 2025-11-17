package hubz.core.exception;

//commit is not present in graph then, CommitNotFoundException
public class CommitNotFoundException extends RuntimeException {
    public CommitNotFoundException(String message) {
        super(message);
    }
}
