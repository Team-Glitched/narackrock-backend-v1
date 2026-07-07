package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortReportPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubmitShortReportUseCaseTest {

    @Test
    void 정상_제출하면_신고_결과를_반환한다() {
        ShortReportPort port = mock(ShortReportPort.class);
        when(port.findActiveShortOwnerId(12L)).thenReturn(Optional.of(99L));
        when(port.existsByReporterAndShort(1L, 12L)).thenReturn(false);
        when(port.save(1L, 12L, "COPYRIGHT", "저작권 침해가 의심됩니다.")).thenReturn(31L);
        SubmitShortReportUseCase useCase = new SubmitShortReportUseCase(port);

        ShortReportResult result = useCase.execute(12L, 1L, "COPYRIGHT", "저작권 침해가 의심됩니다.");

        assertThat(result.reportId()).isEqualTo(31L);
        assertThat(result.shortId()).isEqualTo(12L);
    }

    @Test
    void 숏폼이_없으면_SHORT_NOT_FOUND_예외가_발생하고_이후_단계는_호출되지_않는다() {
        ShortReportPort port = mock(ShortReportPort.class);
        when(port.findActiveShortOwnerId(999L)).thenReturn(Optional.empty());
        SubmitShortReportUseCase useCase = new SubmitShortReportUseCase(port);

        assertThatThrownBy(() -> useCase.execute(999L, 1L, "COPYRIGHT", "설명"))
                .isInstanceOf(ShortReportApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortReportErrorCode.SHORT_NOT_FOUND);

        verify(port, never()).existsByReporterAndShort(any(), any());
        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void 본인_숏폼이면_CANNOT_REPORT_OWN_SHORT_예외가_발생하고_이후_단계는_호출되지_않는다() {
        ShortReportPort port = mock(ShortReportPort.class);
        when(port.findActiveShortOwnerId(12L)).thenReturn(Optional.of(1L));
        SubmitShortReportUseCase useCase = new SubmitShortReportUseCase(port);

        assertThatThrownBy(() -> useCase.execute(12L, 1L, "COPYRIGHT", "설명"))
                .isInstanceOf(ShortReportApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortReportErrorCode.CANNOT_REPORT_OWN_SHORT);

        verify(port, never()).existsByReporterAndShort(any(), any());
        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void 이미_신고했으면_ALREADY_REPORTED_예외가_발생하고_저장은_호출되지_않는다() {
        ShortReportPort port = mock(ShortReportPort.class);
        when(port.findActiveShortOwnerId(12L)).thenReturn(Optional.of(99L));
        when(port.existsByReporterAndShort(1L, 12L)).thenReturn(true);
        SubmitShortReportUseCase useCase = new SubmitShortReportUseCase(port);

        assertThatThrownBy(() -> useCase.execute(12L, 1L, "COPYRIGHT", "설명"))
                .isInstanceOf(ShortReportApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortReportErrorCode.ALREADY_REPORTED);

        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void reason이_공백이면_VALIDATION_ERROR_예외가_발생하고_포트를_호출하지_않는다() {
        ShortReportPort port = mock(ShortReportPort.class);
        SubmitShortReportUseCase useCase = new SubmitShortReportUseCase(port);

        assertThatThrownBy(() -> useCase.execute(12L, 1L, "  ", "설명"))
                .isInstanceOf(ShortReportApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortReportErrorCode.VALIDATION_ERROR);

        verify(port, never()).findActiveShortOwnerId(any());
    }
}
