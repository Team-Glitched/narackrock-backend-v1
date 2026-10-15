package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.GalleryProposalPage;
import java.util.List;

public record GalleryProposalPageResponse(List<GalleryProposalResponse> proposals, Long nextCursor, boolean hasNext) {

    public static GalleryProposalPageResponse from(GalleryProposalPage page) {
        return new GalleryProposalPageResponse(
                page.proposals().stream().map(GalleryProposalResponse::from).toList(),
                page.nextCursor(),
                page.hasNext());
    }
}
