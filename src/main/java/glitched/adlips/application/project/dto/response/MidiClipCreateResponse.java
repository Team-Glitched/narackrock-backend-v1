package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.MidiNote;
import java.util.List;

public record MidiClipCreateResponse(
        Long projectId,
        Long trackId,
        Long clipId,
        ClipType clipType,
        ClipSourceType sourceType,
        String instrument,
        int startTick,
        int durationTick,
        int startTimeMs,
        int durationMs,
        List<MidiNote> midiNotes,
        ApprovalStatus approvalStatus,
        Long trackMediaFileId
) {
}
