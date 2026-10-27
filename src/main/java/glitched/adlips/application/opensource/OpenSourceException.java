package glitched.adlips.application.opensource;

public class OpenSourceException extends RuntimeException {
    public enum Code { INVALID_REQUEST, OPEN_SOURCE_NOT_FOUND }
    private final Code code;

    public OpenSourceException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() { return code; }
}
