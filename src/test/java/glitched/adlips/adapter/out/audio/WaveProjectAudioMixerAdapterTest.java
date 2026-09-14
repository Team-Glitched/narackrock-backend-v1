package glitched.adlips.adapter.out.audio;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.project.port.out.ProjectAudioMixerPort.AudioLayer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;

class WaveProjectAudioMixerAdapterTest {

    @Test
    void mixesPcmWaveAtTimelinePosition() throws Exception {
        WaveProjectAudioMixerAdapter adapter = new WaveProjectAudioMixerAdapter();
        byte[] source = waveWithConstantSample(100, (short) 1_000);

        var result = adapter.mix(List.of(
                new AudioLayer(source, 500, 0, 100, 100, 0)));

        try (AudioInputStream mixed = AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(result.content()))) {
            assertThat(mixed.getFormat().getSampleRate()).isEqualTo(44_100f);
            assertThat(mixed.getFormat().getChannels()).isEqualTo(2);
            assertThat(mixed.getFrameLength()).isEqualTo(26_460L);
        }
        assertThat(result.durationMs()).isEqualTo(600);
    }

    private byte[] waveWithConstantSample(int durationMs, short sample) throws Exception {
        AudioFormat format = new AudioFormat(44_100, 16, 2, true, false);
        int frameCount = 44_100 * durationMs / 1_000;
        byte[] pcm = new byte[frameCount * format.getFrameSize()];
        for (int index = 0; index < pcm.length; index += 2) {
            pcm[index] = (byte) (sample & 0xff);
            pcm[index + 1] = (byte) ((sample >>> 8) & 0xff);
        }
        try (AudioInputStream input = new AudioInputStream(
                new ByteArrayInputStream(pcm), format, frameCount);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        }
    }
}
