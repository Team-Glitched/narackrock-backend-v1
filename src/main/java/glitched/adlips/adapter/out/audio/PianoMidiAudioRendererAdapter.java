package glitched.adlips.adapter.out.audio;

import glitched.adlips.application.project.port.out.MidiAudioRendererPort;
import glitched.adlips.domain.project.MidiNote;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.springframework.stereotype.Component;

@Component
public class PianoMidiAudioRendererAdapter implements MidiAudioRendererPort {
    private static final int SAMPLE_RATE = 44_100;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    @Override
    public byte[] render(List<MidiNote> notes, int bpm, int ppq, int durationMs) {
        if (notes == null || bpm <= 0 || ppq <= 0 || durationMs <= 0 || durationMs > 60_000) {
            throw new IllegalArgumentException("MIDI 렌더링 범위가 올바르지 않습니다.");
        }
        int frames = Math.max(1, Math.round(durationMs * SAMPLE_RATE / 1_000f));
        float[] samples = new float[frames];
        for (MidiNote note : notes) {
            int start = Math.toIntExact((long) note.startTick() * 60_000 * SAMPLE_RATE / (bpm * (long) ppq * 1_000));
            int length = Math.toIntExact((long) note.durationTick() * 60_000 * SAMPLE_RATE / (bpm * (long) ppq * 1_000));
            double frequency = 440.0 * Math.pow(2.0, (note.pitch() - 69) / 12.0);
            double amplitude = note.velocity() / 127.0 * 0.28;
            for (int offset = 0; offset < length && start + offset < frames; offset++) {
                double seconds = offset / (double) SAMPLE_RATE;
                double attack = Math.min(1.0, offset / (SAMPLE_RATE * 0.008));
                double release = Math.min(1.0, (length - offset) / (SAMPLE_RATE * 0.05));
                double decay = Math.exp(-2.0 * seconds / Math.max(0.2, length / (double) SAMPLE_RATE));
                double phase = 2.0 * Math.PI * frequency * seconds;
                double tone = Math.sin(phase) + 0.25 * Math.sin(2 * phase) + 0.08 * Math.sin(3 * phase);
                samples[start + offset] += (float) (tone * amplitude * attack * release * decay);
            }
        }
        byte[] pcm = new byte[frames * 2];
        for (int frame = 0; frame < frames; frame++) {
            short value = (short) Math.round(Math.max(-1.0, Math.min(1.0, samples[frame])) * Short.MAX_VALUE);
            pcm[frame * 2] = (byte) (value & 0xff);
            pcm[frame * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(pcm), FORMAT, frames);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("MIDI 오디오를 생성하지 못했습니다.", exception);
        }
    }
}
