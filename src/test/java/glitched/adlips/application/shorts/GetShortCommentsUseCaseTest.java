package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortCommentQueryItem;
import glitched.adlips.application.shorts.port.out.ShortCommentQueryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetShortCommentsUseCaseTest {

    @Test
    void 정상_조회하면_shortId와_댓글_목록을_반환한다() {
        ShortCommentQueryPort port = mock(ShortCommentQueryPort.class);
        when(port.existsActiveShort(15L)).thenReturn(true);
        ShortCommentQueryItem item = new ShortCommentQueryItem(
                7721L, null, "멜로디가 좋아요.", 12L, "adlip_user", "https://cdn.test/img.png",
                4, 2, false, LocalDateTime.of(2026, 6, 22, 14, 0));
        when(port.findComments(15L, 3L)).thenReturn(List.of(item));
        GetShortCommentsUseCase useCase = new GetShortCommentsUseCase(port);

        ShortCommentListResult result = useCase.execute(15L, 3L);

        assertThat(result.shortId()).isEqualTo(15L);
        assertThat(result.comments()).containsExactly(item);
    }

    @Test
    void 숏폼이_없으면_SHORT_NOT_FOUND_예외가_발생하고_조회를_호출하지_않는다() {
        ShortCommentQueryPort port = mock(ShortCommentQueryPort.class);
        when(port.existsActiveShort(999L)).thenReturn(false);
        GetShortCommentsUseCase useCase = new GetShortCommentsUseCase(port);

        assertThatThrownBy(() -> useCase.execute(999L, null))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.SHORT_NOT_FOUND);
    }
}
