package glitched.adlips.application.project.dto.request;

public record OpenSourceClipCreateRequest(
        Long projectId,
        Long trackId,
        Long userId,
        Long openSourceId,
        int startTick,
        long durationTick
) {
}
