package glitched.adlips.application.project.dto.request;

public record TrackVolumeUpdateRequest(Long trackId, Long userId, int volume) {
}
