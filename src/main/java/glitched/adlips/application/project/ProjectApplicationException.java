package glitched.adlips.application.project;

public class ProjectApplicationException extends RuntimeException {
    private final ProjectErrorCode errorCode;

    public ProjectApplicationException(ProjectErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProjectErrorCode getErrorCode() {
        return errorCode;
    }
}
