package glitched.adlips.domain.reaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReactionToggleTest {

    @Test
    void 반응이_없으면_요청한_반응을_추가한다() {
        ReactionToggle toggle = ReactionToggle.of(null, ReactionType.LIKE);

        assertThat(toggle.result()).isEqualTo(ReactionType.LIKE);
        assertThat(toggle.likeDelta()).isEqualTo(1);
        assertThat(toggle.dislikeDelta()).isZero();
    }

    @Test
    void 같은_반응을_다시_요청하면_취소한다() {
        ReactionToggle toggle = ReactionToggle.of(ReactionType.DISLIKE, ReactionType.DISLIKE);

        assertThat(toggle.result()).isNull();
        assertThat(toggle.likeDelta()).isZero();
        assertThat(toggle.dislikeDelta()).isEqualTo(-1);
    }

    @Test
    void 반대_반응을_요청하면_전환한다() {
        ReactionToggle toggle = ReactionToggle.of(ReactionType.DISLIKE, ReactionType.LIKE);

        assertThat(toggle.result()).isEqualTo(ReactionType.LIKE);
        assertThat(toggle.likeDelta()).isEqualTo(1);
        assertThat(toggle.dislikeDelta()).isEqualTo(-1);
    }

    @Test
    void 요청한_반응은_필수다() {
        assertThatThrownBy(() -> ReactionToggle.of(null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
