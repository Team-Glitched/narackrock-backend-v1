package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryQueryItem;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public record GetManageableGalleriesUseCase(
        GalleryQueryPort galleries, UserBanQueryPort bans, TransactionRunner transactions, Clock clock
) {
    public List<GalleryQueryItem> execute(Long userId) {
        return transactions.readOnly(() -> bans.isBanned(userId, LocalDateTime.now(clock))
                ? List.of() : galleries.findManageableGalleries(userId));
    }
}
