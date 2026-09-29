package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.MidiNote;
import java.util.List;

public interface MidiAudioRendererPort {
    byte[] render(List<MidiNote> notes, int bpm, int ppq, int durationMs);
}
