package glitched.adlips.application.community;

public class GalleryProposalApplicationException extends RuntimeException {

    private final GalleryProposalErrorCode errorCode;

    public GalleryProposalApplicationException(GalleryProposalErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GalleryProposalErrorCode getErrorCode() {
        return errorCode;
    }
}
