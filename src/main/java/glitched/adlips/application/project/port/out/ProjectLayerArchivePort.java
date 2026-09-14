package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.MidiNote;
import java.util.List;

public interface ProjectLayerArchivePort {

    byte[] archive(List<Layer> layers);

    record Layer(
            Long trackId,
            Long clipId,
            ClipType clipType,
            String originalFilename,
            byte[] content,
            List<MidiNote> midiNotes
    ) {
    }
}
