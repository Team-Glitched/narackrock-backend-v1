package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ClipType;

public record AudioClipCreateResponse(
        Long projectId,
        Long trackId,
        Long clipId,
        ClipType clipType,
        ClipSourceType sourceType,
        Long mediaFileId,
        String mediaUrl,
        String waveformUrl,
        int startTick,
        int durationTick,
        int startTimeMs,
        int durationMs,
        int clipOffsetMs,
        ApprovalStatus approvalStatus,
        Long trackMediaFileId
) {
}
