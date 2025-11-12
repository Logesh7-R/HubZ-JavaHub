package hubz.core.exception;

public class RepositoryInitException extends Exception {
    public RepositoryInitException(String message) {
        super(message);
    }

    public RepositoryInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
