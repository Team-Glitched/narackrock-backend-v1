package glitched.adlips.adapter.out.audio;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.project.MidiNote;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;

class SampledMidiAudioRendererAdapterTest {
    @Test
    void createsPlayableWaveWithAudibleMidiNote() throws Exception {
        byte[] wave = new SampledMidiAudioRendererAdapter().render("piano",
                List.of(new MidiNote(60, 0, 480, 100, null, Map.of())), 120, 480, 1_000);

        try (var stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wave))) {
            assertThat(stream.getFormat().getSampleRate()).isEqualTo(44_100f);
            byte[] pcm = stream.readAllBytes();
            assertThat(pcm).containsAnyOf((byte) 1, (byte) 2, (byte) 3, (byte) -1);
            assertThat(stream.getFrameLength()).isEqualTo(44_100);
        }
    }

    @Test
    void usesTrackInstrumentInsteadOfAlwaysRenderingPiano() throws Exception {
        List<MidiNote> notes = List.of(new MidiNote(60, 0, 480, 100, null, Map.of()));
        var renderer = new SampledMidiAudioRendererAdapter();

        byte[] piano = renderer.render("piano", notes, 120, 480, 1_000);
        byte[] guitar = renderer.render("acoustic-guitar", notes, 120, 480, 1_000);
        byte[] legacyPiano = renderer.render("PIANO", notes, 120, 480, 1_000);

        assertThat(guitar).isNotEqualTo(piano);
        assertThat(legacyPiano).isEqualTo(piano);
    }

    @Test
    void rendersDrumNotesAsPercussion() throws Exception {
        byte[] wave = new SampledMidiAudioRendererAdapter().render("drum-kit",
                List.of(new MidiNote(36, 0, 120, 100, null, Map.of())), 120, 480, 2_000);

        try (var stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wave))) {
            assertThat(stream.getFrameLength()).isEqualTo(88_200);
            assertThat(stream.readAllBytes()).containsAnyOf((byte) 1, (byte) 2, (byte) -1);
        }
    }

    /**
     * Entries inside the packaged boot jar are inflater streams without mark/reset; AudioSystem rewinds after sniffing
     * the header, so every export with a MIDI track failed in production while filesystem runs passed.
     */
    @Test
    void readsSamplesFromStreamsThatCannotRewindLikePackagedJarEntries() throws Exception {
        SampledMidiAudioRendererAdapter.ResourceOpener jarLike = path -> {
            InputStream stream = SampledMidiAudioRendererAdapter.class.getClassLoader().getResourceAsStream(path);
            return stream == null ? null : new FilterInputStream(stream) {
                @Override
                public boolean markSupported() {
                    return false;
                }

                @Override
                public synchronized void mark(int readLimit) {
                }

                @Override
                public synchronized void reset() throws IOException {
                    throw new IOException("mark/reset not supported");
                }
            };
        };

        byte[] wave = new SampledMidiAudioRendererAdapter(jarLike).render("marimba",
                List.of(new MidiNote(84, 0, 480, 100, null, Map.of())), 120, 480, 1_000);

        try (var stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wave))) {
            assertThat(stream.readAllBytes()).containsAnyOf((byte) 1, (byte) 2, (byte) -1);
        }
    }
}
