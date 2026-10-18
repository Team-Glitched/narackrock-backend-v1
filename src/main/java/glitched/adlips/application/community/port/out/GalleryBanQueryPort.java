package glitched.adlips.application.community.port.out;

import java.time.LocalDateTime;

/** 갤러리 이용정지·추방 집행용 조회 포트. 이용정지와 추방 모두 "제한"에 해당한다. */
public interface GalleryBanQueryPort {
    boolean isRestrictedInGallery(Long galleryId, Long userId, LocalDateTime now);

    /** 게시글이 속한 갤러리 기준. 게시글이 없으면 false. */
    boolean isRestrictedForPost(Long postId, Long userId, LocalDateTime now);

    /** 댓글이 달린 게시글의 갤러리 기준. 댓글이 없으면 false. */
    boolean isRestrictedForComment(Long commentId, Long userId, LocalDateTime now);

    boolean isExpelled(Long galleryId, Long userId, LocalDateTime now);

    static GalleryBanQueryPort none() {
        return new GalleryBanQueryPort() {
            @Override
            public boolean isRestrictedInGallery(Long galleryId, Long userId, LocalDateTime now) {
                return false;
            }

            @Override
            public boolean isRestrictedForPost(Long postId, Long userId, LocalDateTime now) {
                return false;
            }

            @Override
            public boolean isRestrictedForComment(Long commentId, Long userId, LocalDateTime now) {
                return false;
            }

            @Override
            public boolean isExpelled(Long galleryId, Long userId, LocalDateTime now) {
                return false;
            }
        };
    }
}
