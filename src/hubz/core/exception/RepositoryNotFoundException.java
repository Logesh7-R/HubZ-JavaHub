package hubz.core.exception;

//RepositoryNotFoundException: If repo not found in root directory
public class RepositoryNotFoundException extends Exception {
    public RepositoryNotFoundException(String message) {
        super(message);
    }
}
