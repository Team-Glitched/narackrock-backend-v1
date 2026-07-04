package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.MidiClipSaveRequest;
import glitched.adlips.application.project.dto.response.MidiClipSaveResponse;
import glitched.adlips.application.project.usecase.MidiClipSaveUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class MidiClipControllerTest {
    @Test
    void returnsSavedMidiClip() {
        MidiClipSaveUseCase useCase = mock(MidiClipSaveUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(useCase.execute(any())).thenReturn(
                new MidiClipSaveResponse(1003L, 1, null, "2026-07-05T10:00:00"));

        var result = new MidiClipController(useCase, resolver).save(
                1003L, "Bearer token", new MidiClipSaveRequest(null, null, 0, 1920, List.of()));

        assertThat(result.message()).isEqualTo("MIDI 클립이 저장되었습니다.");
        assertThat(result.data().clipId()).isEqualTo(1003L);
    }
}
