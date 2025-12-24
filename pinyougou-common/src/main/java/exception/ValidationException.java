package exception;

public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super("400", message);
    }

    public ValidationException(String message, Throwable cause) {
        super("400", message, cause);
    }
}
