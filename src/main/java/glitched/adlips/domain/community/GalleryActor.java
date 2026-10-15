package glitched.adlips.domain.community;

import glitched.adlips.domain.user.UserRole;
import java.util.Objects;

/**
 * 특정 갤러리 안에서 본 사용자의 권한 상태.
 *
 * @param galleryRole 해당 갤러리에서의 역할. 없으면 null
 * @param globallyBanned 서비스 전역 이용정지 여부
 */
public record GalleryActor(Long userId, UserRole userRole, GalleryRole galleryRole, boolean globallyBanned) {

    public GalleryActor {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(userRole, "userRole must not be null");
    }

    public boolean isAdmin() {
        return userRole == UserRole.ADMIN;
    }

    public boolean isManager() {
        return galleryRole == GalleryRole.MANAGER;
    }
}
