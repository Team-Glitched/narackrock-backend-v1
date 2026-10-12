package glitched.adlips.application.community;

import glitched.adlips.domain.community.GalleryProposalStatus;
import java.time.LocalDateTime;

/** galleryId는 승인된 경우에만 존재한다. */
public record GalleryProposalReviewResult(
        Long proposalId,
        GalleryProposalStatus status,
        Long galleryId,
        LocalDateTime reviewedAt
) {
}
