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
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class HtiMidiTransformControllerTest {
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
        verify(useCase).execute(any());
    }
}
