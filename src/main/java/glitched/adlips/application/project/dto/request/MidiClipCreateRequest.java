package glitched.adlips.application.project.dto.request;

import java.util.List;

public record MidiClipCreateRequest(
        Long projectId,
        Long trackId,
        Long userId,
        int startTick,
        int durationTick,
        List<MidiNoteRequest> midiNotes
) {
}
