package glitched.adlips.adapter.out.archive;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort.Layer;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.MidiNote;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class ZipProjectLayerArchiveAdapterTest {

    @Test
    void archivesAudioFilesAndMidiNotesByTrack() throws Exception {
        ZipProjectLayerArchiveAdapter adapter = new ZipProjectLayerArchiveAdapter(new Gson());
        var layers = List.of(
                new Layer(11L, 21L, ClipType.AUDIO, "guitar.wav", new byte[]{1, 2}, List.of()),
                new Layer(12L, 22L, ClipType.MIDI, null, null,
                        List.of(new MidiNote(60, 0, 480, 100, null, Map.of()))));

        byte[] archive = adapter.archive(layers);

        Map<String, byte[]> entries = unzip(archive);
        assertThat(entries).containsKeys(
                "tracks/11/audio/21-guitar.wav",
                "tracks/12/midi/22.json");
        assertThat(new String(entries.get("tracks/12/midi/22.json"), StandardCharsets.UTF_8))
                .contains("\"pitch\":60");
    }

    private Map<String, byte[]> unzip(byte[] archive) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (var entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                entries.put(entry.getName(), input.readAllBytes());
            }
        }
        return entries;
    }
}
