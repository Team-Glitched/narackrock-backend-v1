package glitched.adlips.domain.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import jakarta.persistence.Table;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ShortCommentTest {

    @Test
    void ERD에_정의된_조회_인덱스를_매핑한다() {
        Table table = ShortComment.class.getAnnotation(Table.class);

        assertThat(Arrays.stream(table.indexes()).map(index -> index.columnList()))
                .containsExactlyInAnyOrder("shorts_id, created_at", "parent_comment_id", "user_id");
    }

    private ShortForm shorts() {
        return new ShortForm();
    }

    private User user() {
        return User.create("commenter@example.com").withId(1L);
    }

    @Test
    void 필드를_전달하면_그대로_보관하고_parentComment가_없으면_parentCommentId는_null이다() {
        ShortComment comment = new ShortComment(shorts(), user(), "멜로디가 좋아요.", null);

        assertThat(comment.getContent()).isEqualTo("멜로디가 좋아요.");
        assertThat(comment.getParentCommentId()).isNull();
    }

    @Test
    void parentComment를_전달하면_예외없이_생성된다() {
        ShortComment parent = new ShortComment(shorts(), user(), "부모 댓글", null);

        assertThatCode(() -> new ShortComment(shorts(), user(), "저도 이 부분 좋다고 생각해요.", parent))
                .doesNotThrowAnyException();
    }

    @Test
    void shorts가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new ShortComment(null, user(), "내용", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void user가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new ShortComment(shorts(), null, "내용", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void content가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new ShortComment(shorts(), user(), null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void content가_공백이면_예외가_발생한다() {
        assertThatThrownBy(() -> new ShortComment(shorts(), user(), "   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUserId는_생성자에_전달한_user의_id를_반환한다() {
        ShortComment comment = new ShortComment(shorts(), user(), "내용", null);

        assertThat(comment.getUserId()).isEqualTo(1L);
    }

    @Test
    void updateContent로_내용을_수정할_수_있다() {
        ShortComment comment = new ShortComment(shorts(), user(), "원래 내용", null);

        comment.updateContent("수정된 내용");

        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void updateContent에_공백을_전달하면_예외가_발생한다() {
        ShortComment comment = new ShortComment(shorts(), user(), "원래 내용", null);

        assertThatThrownBy(() -> comment.updateContent("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateContent에_null을_전달하면_예외가_발생한다() {
        ShortComment comment = new ShortComment(shorts(), user(), "원래 내용", null);

        assertThatThrownBy(() -> comment.updateContent(null))
                .isInstanceOf(NullPointerException.class);
    }
}
