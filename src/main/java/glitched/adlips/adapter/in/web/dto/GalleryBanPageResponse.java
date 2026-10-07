package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.GalleryBanPage;
import glitched.adlips.application.community.port.out.GalleryBanItem;
import glitched.adlips.domain.community.GalleryBanType;
import java.time.LocalDateTime;
import java.util.List;

public record GalleryBanPageResponse(List<BanItem> bans, Long nextCursor, boolean hasNext) {

    public static GalleryBanPageResponse from(GalleryBanPage page) {
        return new GalleryBanPageResponse(
                page.bans().stream().map(BanItem::from).toList(), page.nextCursor(), page.hasNext());
    }

    public record BanItem(
            Long banId,
            Long userId,
            Long actorId,
            GalleryBanType banType,
            String reason,
            LocalDateTime bannedUntil,
            LocalDateTime liftedAt,
            LocalDateTime createdAt,
            boolean active
    ) {

        static BanItem from(GalleryBanItem item) {
            return new BanItem(item.banId(), item.userId(), item.actorId(), item.banType(), item.reason(),
                    item.bannedUntil(), item.liftedAt(), item.createdAt(), item.active());
        }
    }
}
