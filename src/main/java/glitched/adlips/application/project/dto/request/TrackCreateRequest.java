package glitched.adlips.application.project.dto.request;

public record TrackCreateRequest(Long projectId, Long userId, String name, String instrument, int sortOrder) {
}
