package glitched.adlips.application.project.dto.request;

import glitched.adlips.domain.project.MidiNote;
import java.util.List;

public record MidiClipSaveRequest(
        Long clipId,
        Long userId,
        int startTick,
        int durationTick,
        List<MidiNote> midiNotes
) {
}
