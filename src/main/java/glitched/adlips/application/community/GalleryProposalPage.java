package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import java.util.List;

public record GalleryProposalPage(List<GalleryProposalItem> proposals, Long nextCursor, boolean hasNext) {
}
