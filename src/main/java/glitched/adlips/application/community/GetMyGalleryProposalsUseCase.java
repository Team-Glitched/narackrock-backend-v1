package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import java.util.List;

public class GetMyGalleryProposalsUseCase {

    private final GalleryProposalPort port;
    private final TransactionRunner transactionRunner;

    public GetMyGalleryProposalsUseCase(GalleryProposalPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public List<GalleryProposalItem> execute(Long userId) {
        return transactionRunner.readOnly(() -> port.findByProposer(userId));
    }
}
