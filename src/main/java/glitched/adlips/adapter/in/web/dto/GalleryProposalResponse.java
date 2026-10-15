package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.domain.community.GalleryProposalStatus;
import java.time.LocalDateTime;

public record GalleryProposalResponse(
        Long proposalId,
        Long proposerId,
        String name,
        String description,
        GalleryProposalStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {

    public static GalleryProposalResponse from(GalleryProposalItem item) {
        return new GalleryProposalResponse(
                item.proposalId(),
                item.proposerId(),
                item.name(),
                item.description(),
                item.status(),
                item.rejectionReason(),
                item.createdAt(),
                item.reviewedAt());
    }
}
