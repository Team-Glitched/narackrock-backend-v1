package glitched.adlips.application.project.dto.request;

import java.util.List;

public record MidiClipSaveRequest(
        Long clipId,
        Long userId,
        int startTick,
        long durationTick,
        List<MidiNoteRequest> midiNotes
) {
}
