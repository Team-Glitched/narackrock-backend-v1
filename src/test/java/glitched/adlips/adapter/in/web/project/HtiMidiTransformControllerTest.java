package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.response.HtiMidiTransformResponse;
import glitched.adlips.application.project.usecase.HtiMidiTransformUseCase;
import java.util.List;
import glitched.adlips.domain.project.AudioTranscriptionMode;
import org.mockito.ArgumentCaptor;
import glitched.adlips.application.project.dto.request.HtiMidiTransformRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class HtiMidiTransformControllerTest {
    @Test
    void preservesExplicitInstrumentChoice() {
        var useCase = mock(HtiMidiTransformUseCase.class);
        var resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(7L);
        new HtiMidiTransformController(useCase, resolver).transform(10L, "Bearer token",
                new MockMultipartFile("audio", "piano.wav", "audio/wav", new byte[]{1}),
                AudioTranscriptionMode.INSTRUMENT);
        var captured = ArgumentCaptor.forClass(HtiMidiTransformRequest.class);
        verify(useCase).execute(captured.capture());
        assertThat(captured.getValue().soundKind()).isEqualTo(AudioTranscriptionMode.INSTRUMENT);
    }

    @Test
    void sendsRecordedAudioToUseCase() {
        HtiMidiTransformUseCase useCase = mock(HtiMidiTransformUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(7L);
        when(useCase.execute(any())).thenReturn(new HtiMidiTransformResponse(960, List.of()));
        var audio = new MockMultipartFile(
                "audio", "recording.wav", "audio/wav", new byte[]{1, 2, 3});

        var result = new HtiMidiTransformController(useCase, resolver)
                .transform(10L, "Bearer token", audio);

        assertThat(result.message()).isEqualTo("녹음을 MIDI로 변환했습니다.");
        assertThat(result.data().durationTick()).isEqualTo(960);
        var captured = ArgumentCaptor.forClass(HtiMidiTransformRequest.class);
        verify(useCase).execute(captured.capture());
        assertThat(captured.getValue().soundKind()).isEqualTo(AudioTranscriptionMode.AUTO);
    }
}
