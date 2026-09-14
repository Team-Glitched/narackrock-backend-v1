package glitched.adlips.adapter.out.audio;

import glitched.adlips.application.project.port.out.ProjectAudioMixerPort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.springframework.stereotype.Component;

@Component
public class WaveProjectAudioMixerAdapter implements ProjectAudioMixerPort {

    private static final float SAMPLE_RATE = 44_100f;
    private static final int CHANNELS = 2;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int FRAME_SIZE = CHANNELS * SAMPLE_SIZE_BITS / 8;
    private static final AudioFormat OUTPUT_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            FRAME_SIZE,
            SAMPLE_RATE,
            false);

    @Override
    public MixedAudio mix(List<AudioLayer> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("믹싱할 오디오 레이어가 없습니다.");
        }

        List<DecodedLayer> decodedLayers = new ArrayList<>();
        int totalFrames = 0;
        for (AudioLayer layer : layers) {
            validate(layer);
            byte[] pcm = decode(layer.content());
            int startFrame = millisecondsToFrames(layer.timelineStartMs());
            int offsetFrame = millisecondsToFrames(layer.sourceOffsetMs());
            int requestedFrames = millisecondsToFrames(layer.durationMs());
            int availableFrames = Math.max(0, pcm.length / FRAME_SIZE - offsetFrame);
            int mixedFrames = Math.min(requestedFrames, availableFrames);
            if (mixedFrames == 0) {
                continue;
            }
            decodedLayers.add(new DecodedLayer(layer, pcm, startFrame, offsetFrame, mixedFrames));
            totalFrames = Math.max(totalFrames, startFrame + mixedFrames);
        }
        if (decodedLayers.isEmpty()) {
            throw new IllegalArgumentException("재생 가능한 WAV 오디오 레이어가 없습니다.");
        }

        int[] mixedSamples = new int[totalFrames * CHANNELS];
        for (DecodedLayer decoded : decodedLayers) {
            mixLayer(decoded, mixedSamples);
        }

        byte[] pcm = toPcm(mixedSamples);
        byte[] wave = toWave(pcm, totalFrames);
        return new MixedAudio(wave, framesToMilliseconds(totalFrames));
    }

    private byte[] decode(byte[] content) {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(content));
             AudioInputStream converted = AudioSystem.getAudioInputStream(OUTPUT_FORMAT, source)) {
            return converted.readAllBytes();
        } catch (UnsupportedAudioFileException | IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "현재 로컬 Export는 PCM WAV 오디오만 지원합니다.", exception);
        }
    }

    private void mixLayer(DecodedLayer decoded, int[] target) {
        AudioLayer layer = decoded.layer();
        double volume = layer.volume() / 100.0;
        double leftGain = volume * (layer.pan() > 0 ? 1.0 - layer.pan() / 100.0 : 1.0);
        double rightGain = volume * (layer.pan() < 0 ? 1.0 + layer.pan() / 100.0 : 1.0);
        for (int frame = 0; frame < decoded.frameCount(); frame++) {
            int sourceByte = (decoded.offsetFrame() + frame) * FRAME_SIZE;
            int targetSample = (decoded.startFrame() + frame) * CHANNELS;
            target[targetSample] += (int) (readShort(decoded.pcm(), sourceByte) * leftGain);
            target[targetSample + 1] += (int) (readShort(decoded.pcm(), sourceByte + 2) * rightGain);
        }
    }

    private byte[] toPcm(int[] samples) {
        byte[] pcm = new byte[samples.length * 2];
        for (int index = 0; index < samples.length; index++) {
            int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, samples[index]));
            pcm[index * 2] = (byte) (clamped & 0xff);
            pcm[index * 2 + 1] = (byte) ((clamped >>> 8) & 0xff);
        }
        return pcm;
    }

    private byte[] toWave(byte[] pcm, int frameCount) {
        try (AudioInputStream input = new AudioInputStream(
                new ByteArrayInputStream(pcm), OUTPUT_FORMAT, frameCount);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("믹싱 WAV 생성에 실패했습니다.", exception);
        }
    }

    private short readShort(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xff) | (bytes[offset + 1] << 8));
    }

    private int millisecondsToFrames(int milliseconds) {
        return Math.round(milliseconds * SAMPLE_RATE / 1_000f);
    }

    private int framesToMilliseconds(int frames) {
        return Math.round(frames * 1_000f / SAMPLE_RATE);
    }

    private void validate(AudioLayer layer) {
        if (layer == null || layer.content() == null || layer.content().length == 0
                || layer.timelineStartMs() < 0 || layer.sourceOffsetMs() < 0
                || layer.durationMs() <= 0 || layer.volume() < 0 || layer.volume() > 100
                || layer.pan() < -100 || layer.pan() > 100) {
            throw new IllegalArgumentException("오디오 레이어 설정이 올바르지 않습니다.");
        }
    }

    private record DecodedLayer(
            AudioLayer layer,
            byte[] pcm,
            int startFrame,
            int offsetFrame,
            int frameCount
    ) {
    }
}
