package glitched.adlips.adapter.out.audio;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.MidiNote;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;

class PianoMidiAudioRendererAdapterTest {
    @Test
    void createsPlayableWaveWithAudibleMidiNote() throws Exception {
        byte[] wave = new PianoMidiAudioRendererAdapter().render(
                List.of(new MidiNote(60, 0, 480, 100, null, Map.of())), 120, 480, 1_000);

        try (var stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wave))) {
            assertThat(stream.getFormat().getSampleRate()).isEqualTo(44_100f);
            byte[] pcm = stream.readAllBytes();
            assertThat(pcm).containsAnyOf((byte) 1, (byte) 2, (byte) 3, (byte) -1);
            assertThat(stream.getFrameLength()).isEqualTo(44_100);
        }
    }
}
