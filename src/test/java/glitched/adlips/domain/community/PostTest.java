package glitched.adlips.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PostTest {

    private Gallery gallery() {
        return new Gallery();
    }

    private User user() {
        return User.create("writer@example.com").withId(1L);
    }

    @Test
    void 필드를_전달하면_그대로_보관한다() {
        Post post = new Post(gallery(), user(), "제목", "내용");

        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getViewCount()).isZero();
        assertThat(post.getLikeCount()).isZero();
        assertThat(post.getDislikeCount()).isZero();
        assertThat(post.getCommentCount()).isZero();
        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    void gallery가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new Post(null, user(), "제목", "내용"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void user가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new Post(gallery(), null, "제목", "내용"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void title이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new Post(gallery(), user(), null, "내용"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void title이_공백이면_예외가_발생한다() {
        assertThatThrownBy(() -> new Post(gallery(), user(), "   ", "내용"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void content가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new Post(gallery(), user(), "제목", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void content가_공백이면_예외가_발생한다() {
        assertThatThrownBy(() -> new Post(gallery(), user(), "제목", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUserId는_생성자에_전달한_user의_id를_반환한다() {
        Post post = new Post(gallery(), user(), "제목", "내용");

        assertThat(post.getUserId()).isEqualTo(1L);
    }

    @Test
    void updateTitleAndContent로_제목과_내용을_수정할_수_있다() {
        Post post = new Post(gallery(), user(), "제목", "내용");

        post.updateTitleAndContent("수정된 제목", "수정된 내용");

        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void updateTitleAndContent에_공백_제목을_전달하면_예외가_발생한다() {
        Post post = new Post(gallery(), user(), "제목", "내용");

        assertThatThrownBy(() -> post.updateTitleAndContent("   ", "내용"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateTitleAndContent에_공백_내용을_전달하면_예외가_발생한다() {
        Post post = new Post(gallery(), user(), "제목", "내용");

        assertThatThrownBy(() -> post.updateTitleAndContent("제목", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete로_deletedAt이_반영된다() {
        Post post = new Post(gallery(), user(), "제목", "내용");
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 23, 12, 0);

        post.delete(deletedAt);

        assertThatCode(() -> post.updateTitleAndContent("제목", "내용")).doesNotThrowAnyException();
        assertThat(post.getDeletedAt()).isEqualTo(deletedAt);
    }
}
