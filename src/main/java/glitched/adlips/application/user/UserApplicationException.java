package glitched.adlips.application.user;

public class UserApplicationException extends RuntimeException {
    private final UserErrorCode errorCode;

    public UserApplicationException(UserErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserApplicationException(UserErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public UserErrorCode getErrorCode() {
        return errorCode;
    }
}
