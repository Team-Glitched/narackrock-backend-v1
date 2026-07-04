package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.AudioClipCreateRequest;
import glitched.adlips.application.project.dto.response.AudioClipCreateResponse;
import glitched.adlips.application.project.dto.request.MidiClipCreateRequest;
import glitched.adlips.application.project.dto.response.MidiClipCreateResponse;
import glitched.adlips.application.project.usecase.AudioClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipCreateUseCase;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ClipType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClipControllerTest {
    @Test
    void returnsCreatedForAudioClip() {
        AudioClipCreateUseCase useCase = mock(AudioClipCreateUseCase.class);
        MidiClipCreateUseCase midiUseCase = mock(MidiClipCreateUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(useCase.execute(any())).thenReturn(new AudioClipCreateResponse(
                10L, 21L, 301L, ClipType.AUDIO, ClipSourceType.RECORDING, 501L,
                "media", null, 0, 1920, 0, 2000, 0, ApprovalStatus.DRAFT, null));

        var result = new ClipController(useCase, midiUseCase, resolver).createAudio(
                10L, 21L, "Bearer token", new AudioClipCreateRequest(
                        null, null, null, 501L, ClipSourceType.RECORDING, 0, 1920, 0));

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody().data().clipId()).isEqualTo(301L);
    }

    @Test
    void returnsCreatedForMidiClip() {
        AudioClipCreateUseCase audioUseCase = mock(AudioClipCreateUseCase.class);
        MidiClipCreateUseCase midiUseCase = mock(MidiClipCreateUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(midiUseCase.execute(any())).thenReturn(new MidiClipCreateResponse(
                10L, 103L, 1003L, ClipType.MIDI, ClipSourceType.MIDI_INPUT, "PIANO",
                0, 1920, 0, 2000, List.of(), ApprovalStatus.DRAFT, null));

        var result = new ClipController(audioUseCase, midiUseCase, resolver).createMidi(
                10L, 103L, "Bearer token", new MidiClipCreateRequest(
                        null, null, null, 0, 1920, List.of()));

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody().data().sourceType()).isEqualTo(ClipSourceType.MIDI_INPUT);
    }
}
