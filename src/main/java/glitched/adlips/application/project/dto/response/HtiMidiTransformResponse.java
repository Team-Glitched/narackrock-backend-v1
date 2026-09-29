package glitched.adlips.application.project.dto.response;

import glitched.adlips.application.project.dto.request.MidiNoteRequest;
import java.util.List;

public record HtiMidiTransformResponse(
        int durationTick,
        List<MidiNoteRequest> midiNotes
) {
}
