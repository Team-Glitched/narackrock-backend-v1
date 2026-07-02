package glitched.adlips.application.project.dto.response;

public record TrackVolumeUpdateResponse(
        Long trackId,
        int volume,
        Long trackMediaFileId,
        ProjectVersionResponse projectVersion
) {
}
