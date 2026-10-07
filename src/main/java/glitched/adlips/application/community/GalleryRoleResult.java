package glitched.adlips.application.community;

import glitched.adlips.domain.community.GalleryRole;

/** galleryRole은 해당 갤러리에서 역할이 없으면 null이다. */
public record GalleryRoleResult(Long galleryId, GalleryRole galleryRole, boolean isAdmin, boolean canManage) {
}
