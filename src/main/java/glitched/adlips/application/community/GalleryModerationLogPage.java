package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import java.util.List;

public record GalleryModerationLogPage(List<GalleryModerationLogItem> logs, Long nextCursor, boolean hasNext) {
}
