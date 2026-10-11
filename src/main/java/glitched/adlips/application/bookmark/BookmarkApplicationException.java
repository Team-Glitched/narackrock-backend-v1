package glitched.adlips.application.bookmark;

public class BookmarkApplicationException extends RuntimeException {

    private final BookmarkErrorCode errorCode;

    public BookmarkApplicationException(BookmarkErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BookmarkErrorCode getErrorCode() {
        return errorCode;
    }
}
