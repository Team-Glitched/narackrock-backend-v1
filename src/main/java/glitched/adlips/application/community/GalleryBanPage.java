package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryBanItem;
import java.util.List;

public record GalleryBanPage(List<GalleryBanItem> bans, Long nextCursor, boolean hasNext) {
}
