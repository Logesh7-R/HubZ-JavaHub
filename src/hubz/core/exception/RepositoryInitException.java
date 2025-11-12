package hubz.core.exception;

//RepositoryInitException: If Repo failed to initialize
public class RepositoryInitException extends Exception {
    public RepositoryInitException(String message) {
        super(message);
    }

    public RepositoryInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
