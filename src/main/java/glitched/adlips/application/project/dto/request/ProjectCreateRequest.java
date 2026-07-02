package glitched.adlips.application.project.dto.request;

public record ProjectCreateRequest(
        Long ownerId,
        String title,
        String description,
        Long albumImageFileId
) {
}
