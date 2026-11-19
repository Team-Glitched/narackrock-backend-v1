package glitched.adlips.application.project.dto.response;

import glitched.adlips.application.project.dto.request.MidiNoteRequest;
import java.util.List;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort.Routing;

public record HtiMidiTransformResponse(
        int durationTick,
        List<MidiNoteRequest> midiNotes,
        Routing routing
) {
    public HtiMidiTransformResponse(int durationTick, List<MidiNoteRequest> midiNotes) {
        this(durationTick, midiNotes, null);
    }
}
