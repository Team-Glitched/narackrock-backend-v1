package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.util.List;

public class GetGalleriesUseCase {

    private final GalleryQueryPort port;
    private final TransactionRunner transactionRunner;

    public GetGalleriesUseCase(GalleryQueryPort port) {
        this(port, TransactionRunner.direct());
    }

    public GetGalleriesUseCase(GalleryQueryPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public List<GalleryQueryItem> execute() {
        return transactionRunner.readOnly(port::findGalleries);
    }
}
