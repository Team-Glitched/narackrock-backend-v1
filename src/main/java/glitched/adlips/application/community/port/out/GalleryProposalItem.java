package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryProposalStatus;
import java.time.LocalDateTime;

public record GalleryProposalItem(
        Long proposalId,
        Long proposerId,
        String name,
        String description,
        GalleryProposalStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
