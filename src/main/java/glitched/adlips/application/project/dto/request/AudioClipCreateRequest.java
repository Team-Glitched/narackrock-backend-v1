package glitched.adlips.application.project.dto.request;

import glitched.adlips.domain.project.ClipSourceType;

public record AudioClipCreateRequest(
        Long projectId,
        Long trackId,
        Long userId,
        Long mediaFileId,
        ClipSourceType sourceType,
        int startTick,
        int durationTick,
        int clipOffsetMs
) {
}
