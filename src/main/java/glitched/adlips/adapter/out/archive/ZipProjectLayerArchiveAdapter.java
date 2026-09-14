package glitched.adlips.adapter.out.archive;

import com.google.gson.Gson;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort;
import glitched.adlips.domain.project.ClipType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class ZipProjectLayerArchiveAdapter implements ProjectLayerArchivePort {

    private final Gson gson;

    public ZipProjectLayerArchiveAdapter() {
        this(new Gson());
    }

    ZipProjectLayerArchiveAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public byte[] archive(List<Layer> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("압축할 레이어가 없습니다.");
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Layer layer : layers) {
                writeLayer(zip, layer);
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("레이어 ZIP 생성에 실패했습니다.", exception);
        }
    }

    private void writeLayer(ZipOutputStream zip, Layer layer) throws IOException {
        if (layer.clipType() == ClipType.AUDIO) {
            if (layer.content() == null || layer.content().length == 0) {
                throw new IllegalArgumentException("오디오 레이어 원본이 비어 있습니다.");
            }
            String filename = safeFilename(layer.originalFilename(), "source.wav");
            write(zip, "tracks/" + layer.trackId() + "/audio/" + layer.clipId() + "-" + filename,
                    layer.content());
            return;
        }
        byte[] json = gson.toJson(layer.midiNotes() == null ? List.of() : layer.midiNotes())
                .getBytes(StandardCharsets.UTF_8);
        write(zip, "tracks/" + layer.trackId() + "/midi/" + layer.clipId() + ".json", json);
    }

    private void write(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private String safeFilename(String filename, String fallback) {
        if (filename == null || filename.isBlank()) {
            return fallback;
        }
        String safe = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? fallback : safe;
    }
}
