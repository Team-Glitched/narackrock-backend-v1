package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.domain.community.GalleryProposalStatus;

public record GalleryProposalCreateResponse(Long proposalId, GalleryProposalStatus status) {

    public static GalleryProposalCreateResponse pending(Long proposalId) {
        return new GalleryProposalCreateResponse(proposalId, GalleryProposalStatus.PENDING);
    }
}
