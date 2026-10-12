package glitched.adlips.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;

class GalleryModerationLogTest {

    private Gallery gallery() {
        return Gallery.create("재즈", "재즈 이야기");
    }

    private User actor() {
        return User.create("manager@example.com").withId(1L);
    }

    @Test
    void 관리_이력을_기록한다() {
        GalleryModerationLog log = GalleryModerationLog.record(
                gallery(), actor(), GalleryModerationActionType.DELETE_POST, 2L, 30L, "  광고  ");

        assertThat(log.getActorId()).isEqualTo(1L);
        assertThat(log.getActionType()).isEqualTo(GalleryModerationActionType.DELETE_POST);
        assertThat(log.getTargetUserId()).isEqualTo(2L);
        assertThat(log.getTargetId()).isEqualTo(30L);
        assertThat(log.getReason()).isEqualTo("광고");
    }

    @Test
    void 대상과_사유는_없어도_된다() {
        GalleryModerationLog log = GalleryModerationLog.record(
                gallery(), actor(), GalleryModerationActionType.UPDATE_GALLERY, null, null, null);

        assertThat(log.getTargetUserId()).isNull();
        assertThat(log.getTargetId()).isNull();
        assertThat(log.getReason()).isNull();
    }

    @Test
    void 갤러리_행위자_유형은_필수다() {
        assertThatThrownBy(() -> GalleryModerationLog.record(
                null, actor(), GalleryModerationActionType.UPDATE_GALLERY, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> GalleryModerationLog.record(
                gallery(), null, GalleryModerationActionType.UPDATE_GALLERY, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> GalleryModerationLog.record(gallery(), actor(), null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
