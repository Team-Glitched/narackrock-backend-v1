package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.application.shorts.port.out.UserBanQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitShortCommentUseCaseTest {

    ShortCommentPort port;
    UserBanQueryPort userBanQueryPort;
    TransactionRunner transactionRunner;
    Clock clock;
    SubmitShortCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(ShortCommentPort.class);
        userBanQueryPort = mock(UserBanQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        clock = Clock.fixed(Instant.parse("2026-07-08T10:00:00Z"), ZoneOffset.UTC);
        useCase = new SubmitShortCommentUseCase(port, userBanQueryPort, transactionRunner, clock);
    }

    @Test
    void 정상_댓글_생성시_comment_count만_증가하고_reply_count는_증가하지_않는다() {
        when(port.existsActiveShort(12L)).thenReturn(true);
        when(port.save(12L, 1L, "멜로디가 좋아요.", null)).thenReturn(7721L);

        ShortCommentResult result = useCase.execute(12L, 1L, "멜로디가 좋아요.", null);

        assertThat(result.shortId()).isEqualTo(12L);
        assertThat(result.commentId()).isEqualTo(7721L);
        assertThat(result.parentCommentId()).isNull();
        verify(port).adjustShortCommentCount(12L, 1);
        verify(port, never()).adjustParentReplyCount(any(), anyInt());
    }

    @Test
    void 정상_대댓글_생성시_comment_count와_reply_count가_모두_증가한다() {
        when(port.existsActiveShort(12L)).thenReturn(true);
        when(port.existsActiveParentComment(7721L, 12L)).thenReturn(true);
        when(port.save(12L, 1L, "저도 이 부분 좋다고 생각해요.", 7721L)).thenReturn(7730L);

        ShortCommentResult result = useCase.execute(12L, 1L, "저도 이 부분 좋다고 생각해요.", 7721L);

        assertThat(result.commentId()).isEqualTo(7730L);
        assertThat(result.parentCommentId()).isEqualTo(7721L);
        verify(port).adjustShortCommentCount(12L, 1);
        verify(port).adjustParentReplyCount(7721L, 1);
    }

    @Test
    void 이용_정지_사용자는_BANNED_USER_ACCESS_예외가_발생하고_댓글을_저장하지_않는다() {
        when(userBanQueryPort.isBanned(1L, LocalDateTime.now(clock))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(12L, 1L, "내용", null))
                .isInstanceOf(ShortCommentApplicationException.class)
                .hasMessage("현재 서비스 이용 정지 상태이므로 댓글을 작성할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.BANNED_USER_ACCESS);

        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void 정지된_사용자는_content가_공백이어도_BANNED_USER_ACCESS가_우선_발생한다() {
        when(userBanQueryPort.isBanned(1L, LocalDateTime.now(clock))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(12L, 1L, "  ", null))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.BANNED_USER_ACCESS);
    }

    @Test
    void 댓글_내용이_공백이면_INVALID_INPUT_VALUE_예외와_댓글_메시지가_발생하고_포트를_호출하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(12L, 1L, "  ", null))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> useCase.execute(12L, 1L, "  ", null))
                .hasMessage("댓글 내용은 필수 입력 사항입니다.");

        verify(port, never()).existsActiveShort(any());
    }

    @Test
    void 대댓글_내용이_공백이면_대댓글_메시지가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(12L, 1L, null, 7721L))
                .isInstanceOf(ShortCommentApplicationException.class)
                .hasMessage("대댓글 내용은 필수 입력 사항입니다.")
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 숏폼이_없으면_SHORT_NOT_FOUND_예외가_발생하고_저장은_호출되지_않는다() {
        when(port.existsActiveShort(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(999L, 1L, "내용", null))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.SHORT_NOT_FOUND);

        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void 부모_댓글이_없거나_다른_숏폼_소속이면_PARENT_COMMENT_NOT_FOUND_예외가_발생하고_저장은_호출되지_않는다() {
        when(port.existsActiveShort(12L)).thenReturn(true);
        when(port.existsActiveParentComment(999L, 12L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(12L, 1L, "내용", 999L))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.PARENT_COMMENT_NOT_FOUND);

        verify(port, never()).save(any(), any(), any(), any());
    }
}
