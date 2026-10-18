package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.GalleryRoleResult;
import glitched.adlips.domain.community.GalleryRole;

public record GalleryRoleResponse(Long galleryId, GalleryRole galleryRole, boolean isAdmin, boolean canManage) {

    public static GalleryRoleResponse from(GalleryRoleResult result) {
        return new GalleryRoleResponse(
                result.galleryId(), result.galleryRole(), result.isAdmin(), result.canManage());
    }
}
