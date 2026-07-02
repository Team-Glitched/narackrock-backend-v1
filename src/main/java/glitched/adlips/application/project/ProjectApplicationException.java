package glitched.adlips.application.project;

public class ProjectApplicationException extends RuntimeException {
    private final ProjectErrorCode errorCode;
    private final Object data;

    public ProjectApplicationException(ProjectErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ProjectApplicationException(ProjectErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public ProjectErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
