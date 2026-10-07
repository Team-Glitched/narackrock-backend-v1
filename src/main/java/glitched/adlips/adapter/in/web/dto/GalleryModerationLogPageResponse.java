package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.GalleryModerationLogPage;
import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import glitched.adlips.domain.community.GalleryModerationActionType;
import java.time.LocalDateTime;
import java.util.List;

public record GalleryModerationLogPageResponse(List<LogItem> logs, Long nextCursor, boolean hasNext) {

    public static GalleryModerationLogPageResponse from(GalleryModerationLogPage page) {
        return new GalleryModerationLogPageResponse(
                page.logs().stream().map(LogItem::from).toList(), page.nextCursor(), page.hasNext());
    }

    public record LogItem(
            Long logId,
            GalleryModerationActionType actionType,
            Long actorId,
            Long targetUserId,
            Long targetId,
            String reason,
            LocalDateTime createdAt
    ) {

        static LogItem from(GalleryModerationLogItem item) {
            return new LogItem(item.logId(), item.actionType(), item.actorId(), item.targetUserId(),
                    item.targetId(), item.reason(), item.createdAt());
        }
    }
}
