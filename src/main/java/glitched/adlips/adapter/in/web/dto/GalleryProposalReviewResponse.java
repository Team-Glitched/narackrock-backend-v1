package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.GalleryProposalReviewResult;
import glitched.adlips.domain.community.GalleryProposalStatus;
import java.time.LocalDateTime;

public record GalleryProposalReviewResponse(
        Long proposalId,
        GalleryProposalStatus status,
        Long galleryId,
        LocalDateTime reviewedAt
) {

    public static GalleryProposalReviewResponse from(GalleryProposalReviewResult result) {
        return new GalleryProposalReviewResponse(
                result.proposalId(), result.status(), result.galleryId(), result.reviewedAt());
    }
}
