package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.application.shorts.port.out.ShortCommentReportPort;
import glitched.adlips.domain.shorts.ShortComment;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubmitShortCommentReportUseCaseTest {

    private ShortComment ownedComment(Long ownerId) {
        User owner = User.create("owner@example.com").withId(ownerId);
        return new ShortComment(mock(ShortForm.class), owner, "댓글 내용", null);
    }

    @Test
    void 정상_제출하면_신고_결과를_반환한다() {
        ShortCommentPort commentPort = mock(ShortCommentPort.class);
        ShortCommentReportPort reportPort = mock(ShortCommentReportPort.class);
        when(commentPort.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(99L)));
        when(reportPort.existsByReporterAndComment(1L, 7721L)).thenReturn(false);
        when(reportPort.save(1L, 7721L, "욕설/비방", "특정 사용자를 비방하는 내용이 포함되어 있습니다."))
                .thenReturn(Optional.of(303L));
        SubmitShortCommentReportUseCase useCase =
                new SubmitShortCommentReportUseCase(commentPort, reportPort);

        ShortCommentReportResult result = useCase.execute(
                15L, 7721L, 1L, "욕설/비방", "특정 사용자를 비방하는 내용이 포함되어 있습니다.");

        assertThat(result.reportId()).isEqualTo(303L);
        assertThat(result.commentId()).isEqualTo(7721L);
    }

    @Test
    void reason이_공백이면_INVALID_INPUT_VALUE_예외가_발생하고_포트를_호출하지_않는다() {
        ShortCommentPort commentPort = mock(ShortCommentPort.class);
        ShortCommentReportPort reportPort = mock(ShortCommentReportPort.class);
        SubmitShortCommentReportUseCase useCase =
                new SubmitShortCommentReportUseCase(commentPort, reportPort);

        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L, "  ", "설명"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.INVALID_INPUT_VALUE);

        verify(commentPort, never()).findActiveComment(any(), any());
    }

    @Test
    void 댓글이_없으면_COMMENT_NOT_FOUND_예외가_발생하고_이후_단계는_호출되지_않는다() {
        ShortCommentPort commentPort = mock(ShortCommentPort.class);
        ShortCommentReportPort reportPort = mock(ShortCommentReportPort.class);
        when(commentPort.findActiveComment(999L, 15L)).thenReturn(Optional.empty());
        SubmitShortCommentReportUseCase useCase =
                new SubmitShortCommentReportUseCase(commentPort, reportPort);

        assertThatThrownBy(() -> useCase.execute(15L, 999L, 1L, "욕설/비방", "설명"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.COMMENT_NOT_FOUND);

        verify(reportPort, never()).existsByReporterAndComment(any(), any());
        verify(reportPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void 본인_댓글이면_CANNOT_REPORT_OWN_COMMENT_예외가_발생하고_이후_단계는_호출되지_않는다() {
        ShortCommentPort commentPort = mock(ShortCommentPort.class);
        ShortCommentReportPort reportPort = mock(ShortCommentReportPort.class);
        when(commentPort.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(1L)));
        SubmitShortCommentReportUseCase useCase =
                new SubmitShortCommentReportUseCase(commentPort, reportPort);

        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L, "욕설/비방", "설명"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.CANNOT_REPORT_OWN_COMMENT);

        verify(reportPort, never()).existsByReporterAndComment(any(), any());
        verify(reportPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void 이미_신고했으면_ALREADY_REPORTED_예외가_발생하고_저장은_호출되지_않는다() {
        ShortCommentPort commentPort = mock(ShortCommentPort.class);
        ShortCommentReportPort reportPort = mock(ShortCommentReportPort.class);
        when(commentPort.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(99L)));
        when(reportPort.existsByReporterAndComment(1L, 7721L)).thenReturn(true);
        SubmitShortCommentReportUseCase useCase =
                new SubmitShortCommentReportUseCase(commentPort, reportPort);

        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L, "욕설/비방", "설명"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.ALREADY_REPORTED);

        verify(reportPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void 중복_확인_직후_동시_신고가_저장되면_ALREADY_REPORTED_예외가_발생한다() {
        ShortCommentPort commentPort = mock(ShortCommentPort.class);
        ShortCommentReportPort reportPort = mock(ShortCommentReportPort.class);
        when(commentPort.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(99L)));
        when(reportPort.existsByReporterAndComment(1L, 7721L)).thenReturn(false);
        when(reportPort.save(1L, 7721L, "욕설/비방", "설명")).thenReturn(Optional.empty());
        SubmitShortCommentReportUseCase useCase =
                new SubmitShortCommentReportUseCase(commentPort, reportPort);

        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L, "욕설/비방", "설명"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.ALREADY_REPORTED);
    }
}
