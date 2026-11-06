package glitched.adlips.adapter.out.audio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import glitched.adlips.application.project.port.out.MidiAudioRendererPort;
import glitched.adlips.domain.project.MidiNote;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.springframework.stereotype.Component;

@Component
public class SampledMidiAudioRendererAdapter implements MidiAudioRendererPort {
    private static final int OUTPUT_RATE = 44_100;
    private static final int SOURCE_RATE = 22_050;
    private static final AudioFormat FORMAT = new AudioFormat(OUTPUT_RATE, 16, 1, true, false);
    private static final int RELEASE_FRAMES = OUTPUT_RATE * 8 / 100;
    private static final int DRUM_FRAMES = OUTPUT_RATE * 145 / 100;
    private static final String RESOURCE_ROOT = "audio/instruments/";

    private final ResourceOpener resources;
    private final Map<String, Instrument> instruments;
    private final Map<String, short[]> sampleCache = new ConcurrentHashMap<>();

    /** Opens a classpath-relative resource, returning null when it does not exist. */
    @FunctionalInterface
    interface ResourceOpener {
        InputStream open(String path) throws IOException;
    }

    public SampledMidiAudioRendererAdapter() {
        this(path -> SampledMidiAudioRendererAdapter.class.getClassLoader().getResourceAsStream(path));
    }

    SampledMidiAudioRendererAdapter(ResourceOpener resources) {
        this.resources = resources;
        this.instruments = loadInstruments();
    }

    @Override
    public byte[] render(String instrumentId, List<MidiNote> notes, int bpm, int ppq, int durationMs) {
        if (notes == null || bpm <= 0 || ppq <= 0 || durationMs <= 0 || durationMs > 60_000) {
            throw new IllegalArgumentException("MIDI 렌더링 범위가 올바르지 않습니다.");
        }
        Instrument instrument = instruments.getOrDefault(normalize(instrumentId), instruments.get("piano"));
        int frames = Math.max(1, Math.round(durationMs * OUTPUT_RATE / 1_000f));
        float[] mix = new float[frames];
        for (MidiNote note : notes) {
            if (instrument.drums() && (note.pitch() < 35 || note.pitch() > 81)) {
                continue;
            }
            int start = Math.toIntExact((long) note.startTick() * 60_000 * OUTPUT_RATE
                    / (bpm * (long) ppq * 1_000));
            if (start >= frames) {
                continue;
            }
            int length = instrument.drums() ? DRUM_FRAMES
                    : Math.toIntExact((long) note.durationTick() * 60_000 * OUTPUT_RATE
                    / (bpm * (long) ppq * 1_000));
            if (length <= 0) {
                continue;
            }
            int anchor = nearest(instrument.pitches(), note.pitch());
            short[] sample = sampleCache.computeIfAbsent(instrument.id() + "/" + anchor, key -> loadSample(key));
            double rate = Math.pow(2.0, (note.pitch() - anchor) / 12.0) * SOURCE_RATE / OUTPUT_RATE;
            int playable = Math.min(length, frames - start);
            float gain = note.velocity() / 127f;
            for (int offset = 0; offset < playable; offset++) {
                double position = offset * rate;
                int index = (int) position;
                if (index >= sample.length - 1) {
                    break;
                }
                double fraction = position - index;
                double value = sample[index] * (1.0 - fraction) + sample[index + 1] * fraction;
                double release = Math.min(1.0, (playable - offset) / (double) RELEASE_FRAMES);
                mix[start + offset] += (float) (value / Short.MAX_VALUE * gain * release);
            }
        }
        byte[] pcm = new byte[frames * 2];
        for (int frame = 0; frame < frames; frame++) {
            short value = (short) Math.round(Math.max(-1.0f, Math.min(1.0f, mix[frame])) * Short.MAX_VALUE);
            pcm[frame * 2] = (byte) value;
            pcm[frame * 2 + 1] = (byte) (value >>> 8);
        }
        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(pcm), FORMAT, frames);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("MIDI 오디오를 생성하지 못했습니다.", exception);
        }
    }

    private short[] loadSample(String key) {
        String path = RESOURCE_ROOT + key + ".wav";
        try (InputStream resource = resources.open(path)) {
            if (resource == null) {
                throw new IllegalStateException("악기 샘플이 없습니다: " + path);
            }
            // AudioSystem rewinds after sniffing the header. Packaged-jar entries are inflater streams without
            // mark/reset (filesystem resources are already buffered), so buffer them to read the same way in both.
            try (AudioInputStream wave = AudioSystem.getAudioInputStream(new BufferedInputStream(resource))) {
                AudioFormat format = wave.getFormat();
                if (format.getSampleRate() != SOURCE_RATE || format.getChannels() != 1
                        || format.getSampleSizeInBits() != 16 || format.isBigEndian()) {
                    throw new IllegalStateException("지원하지 않는 악기 샘플 형식: " + path);
                }
                byte[] pcm = wave.readAllBytes();
                short[] samples = new short[pcm.length / 2];
                for (int index = 0; index < samples.length; index++) {
                    samples[index] = (short) ((pcm[index * 2] & 0xff) | (pcm[index * 2 + 1] << 8));
                }
                return samples;
            }
        } catch (IOException | UnsupportedAudioFileException exception) {
            throw new IllegalStateException("악기 샘플을 읽지 못했습니다: " + path, exception);
        }
    }

    private Map<String, Instrument> loadInstruments() {
        try (InputStream input = resources.open(RESOURCE_ROOT + "manifest.json")) {
            if (input == null) {
                throw new IllegalStateException("악기 목록이 없습니다.");
            }
            JsonNode definitions = new ObjectMapper().readTree(input);
            Map<String, Instrument> result = new HashMap<>();
            for (JsonNode definition : definitions) {
                boolean drums = "drum".equals(definition.path("kind").asText());
                List<Integer> pitches = new ArrayList<>();
                if (drums) {
                    for (int pitch = definition.path("minPitch").asInt();
                         pitch <= definition.path("maxPitch").asInt(); pitch++) {
                        pitches.add(pitch);
                    }
                } else {
                    definition.path("pitches").forEach(value -> pitches.add(value.asInt()));
                }
                String id = definition.path("id").asText();
                result.put(id, new Instrument(id, drums, List.copyOf(pitches)));
            }
            if (!result.containsKey("piano")) {
                throw new IllegalStateException("피아노 악기 설정이 없습니다.");
            }
            return Map.copyOf(result);
        } catch (IOException exception) {
            throw new IllegalStateException("악기 목록을 읽지 못했습니다.", exception);
        }
    }

    private int nearest(List<Integer> pitches, int note) {
        int best = pitches.getFirst();
        for (int pitch : pitches) {
            if (Math.abs(note - pitch) < Math.abs(note - best)) {
                best = pitch;
            }
        }
        return best;
    }

    private String normalize(String instrumentId) {
        if (instrumentId == null) return "piano";
        return switch (instrumentId.trim()) {
            case "PIANO", "피아노" -> "piano";
            case "GUITAR", "기타" -> "acoustic-guitar";
            case "BASS", "베이스" -> "electric-bass";
            case "DRUM", "DRUMS", "드럼" -> "drum-kit";
            default -> instrumentId.trim();
        };
    }

    private record Instrument(String id, boolean drums, List<Integer> pitches) { }
}
