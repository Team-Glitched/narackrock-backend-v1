package glitched.adlips.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GalleryBanTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);

    private Gallery gallery() {
        return Gallery.create("재즈", "재즈 이야기");
    }

    private User target() {
        return User.create("target@example.com").withId(2L);
    }

    private User actor() {
        return User.create("manager@example.com").withId(1L);
    }

    @Test
    void 이용정지는_기간만큼_만료_시각을_정한다() {
        GalleryBan ban = GalleryBan.suspend(gallery(), target(), actor(), "  도배  ", 7, NOW);

        assertThat(ban.getBanType()).isEqualTo(GalleryBanType.SUSPEND);
        assertThat(ban.getUserId()).isEqualTo(2L);
        assertThat(ban.getActorId()).isEqualTo(1L);
        assertThat(ban.getReason()).isEqualTo("도배");
        assertThat(ban.getBannedUntil()).isEqualTo(NOW.plusDays(7));
        assertThat(ban.getLiftedAt()).isNull();
    }

    @Test
    void 이용정지_기간은_1일에서_30일_사이여야_한다() {
        assertThatThrownBy(() -> GalleryBan.suspend(gallery(), target(), actor(), "사유", 0, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GalleryBan.suspend(gallery(), target(), actor(), "사유", 31, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 추방은_만료_시각이_없다() {
        GalleryBan ban = GalleryBan.expel(gallery(), target(), actor(), "사유");

        assertThat(ban.getBanType()).isEqualTo(GalleryBanType.EXPEL);
        assertThat(ban.getBannedUntil()).isNull();
        assertThat(ban.isActiveAt(NOW.plusYears(10))).isTrue();
    }

    @Test
    void 사유가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> GalleryBan.expel(gallery(), target(), actor(), "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GalleryBan.suspend(gallery(), target(), actor(), null, 1, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 대상_행위자_갤러리가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> GalleryBan.expel(null, target(), actor(), "사유"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> GalleryBan.expel(gallery(), null, actor(), "사유"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> GalleryBan.expel(gallery(), target(), null, "사유"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void 이용정지는_만료_시각_직전까지만_유효하다() {
        GalleryBan ban = GalleryBan.suspend(gallery(), target(), actor(), "사유", 1, NOW);

        assertThat(ban.isActiveAt(NOW.plusDays(1).minusSeconds(1))).isTrue();
        assertThat(ban.isActiveAt(NOW.plusDays(1))).isFalse();
    }

    @Test
    void 해제하면_더_이상_유효하지_않고_다시_해제해도_처음_기록을_유지한다() {
        GalleryBan ban = GalleryBan.expel(gallery(), target(), actor(), "사유");
        User otherActor = User.create("admin@example.com").withId(9L);

        ban.lift(actor(), NOW);
        ban.lift(otherActor, NOW.plusDays(1));

        assertThat(ban.isActiveAt(NOW)).isFalse();
        assertThat(ban.getLiftedAt()).isEqualTo(NOW);
        assertThat(ban.getLiftedById()).isEqualTo(1L);
    }
}
