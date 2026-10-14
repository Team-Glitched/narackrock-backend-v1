package glitched.adlips.domain.community;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.user.UserRole;
import org.junit.jupiter.api.Test;

class GalleryModerationPolicyTest {

    private static final GalleryActor USER = new GalleryActor(1L, UserRole.USER, null, false);
    private static final GalleryActor OTHER_USER = new GalleryActor(2L, UserRole.USER, null, false);
    private static final GalleryActor MANAGER = new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false);
    private static final GalleryActor BANNED_MANAGER = new GalleryActor(4L, UserRole.USER, GalleryRole.MANAGER, true);
    private static final GalleryActor ADMIN = new GalleryActor(5L, UserRole.ADMIN, null, false);
    private static final GalleryActor OTHER_ADMIN = new GalleryActor(6L, UserRole.ADMIN, null, false);
    private static final GalleryActor BANNED_ADMIN = new GalleryActor(7L, UserRole.ADMIN, null, true);

    @Test
    void 관리_권한은_해당_갤러리_매니저와_어드민에게만_있다() {
        assertThat(GalleryModerationPolicy.canManage(MANAGER)).isTrue();
        assertThat(GalleryModerationPolicy.canManage(ADMIN)).isTrue();
        assertThat(GalleryModerationPolicy.canManage(USER)).isFalse();
    }

    @Test
    void 서비스_전역_정지_상태면_매니저와_어드민도_관리할_수_없다() {
        assertThat(GalleryModerationPolicy.canManage(BANNED_MANAGER)).isFalse();
        assertThat(GalleryModerationPolicy.canManage(BANNED_ADMIN)).isFalse();
    }

    @Test
    void 매니저는_일반_이용자만_제재할_수_있다() {
        assertThat(GalleryModerationPolicy.canSanction(MANAGER, USER)).isTrue();
        assertThat(GalleryModerationPolicy.canSanction(MANAGER, MANAGER)).isFalse();
        assertThat(GalleryModerationPolicy.canSanction(MANAGER, BANNED_MANAGER)).isFalse();
        assertThat(GalleryModerationPolicy.canSanction(MANAGER, ADMIN)).isFalse();
    }

    @Test
    void 어드민은_매니저를_제재할_수_있지만_본인이나_다른_어드민은_제재할_수_없다() {
        assertThat(GalleryModerationPolicy.canSanction(ADMIN, USER)).isTrue();
        assertThat(GalleryModerationPolicy.canSanction(ADMIN, MANAGER)).isTrue();
        assertThat(GalleryModerationPolicy.canSanction(ADMIN, ADMIN)).isFalse();
        assertThat(GalleryModerationPolicy.canSanction(ADMIN, OTHER_ADMIN)).isFalse();
    }

    @Test
    void 관리_권한이_없으면_누구도_제재할_수_없다() {
        assertThat(GalleryModerationPolicy.canSanction(USER, OTHER_USER)).isFalse();
        assertThat(GalleryModerationPolicy.canSanction(BANNED_MANAGER, USER)).isFalse();
    }

    @Test
    void 매니저는_어드민이_쓴_글을_삭제할_수_없다() {
        assertThat(GalleryModerationPolicy.canDeleteContentOf(MANAGER, USER)).isTrue();
        assertThat(GalleryModerationPolicy.canDeleteContentOf(MANAGER, MANAGER)).isTrue();
        assertThat(GalleryModerationPolicy.canDeleteContentOf(MANAGER, ADMIN)).isFalse();
    }

    @Test
    void 어드민은_모든_작성자의_글을_삭제할_수_있다() {
        assertThat(GalleryModerationPolicy.canDeleteContentOf(ADMIN, USER)).isTrue();
        assertThat(GalleryModerationPolicy.canDeleteContentOf(ADMIN, OTHER_ADMIN)).isTrue();
    }

    @Test
    void 관리_권한이_없으면_글을_삭제할_수_없다() {
        assertThat(GalleryModerationPolicy.canDeleteContentOf(USER, OTHER_USER)).isFalse();
        assertThat(GalleryModerationPolicy.canDeleteContentOf(BANNED_MANAGER, USER)).isFalse();
    }
}
