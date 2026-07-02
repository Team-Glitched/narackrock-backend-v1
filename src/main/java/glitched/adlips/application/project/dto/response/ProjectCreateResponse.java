package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ProjectStatus;
import java.time.LocalDateTime;

public record ProjectCreateResponse(
        Long projectId,
        String title,
        String description,
        Long albumImageFileId,
        String albumImageUrl,
        Long ownerId,
        ProjectVersionResponse version,
        ProjectStatus status,
        boolean isPublic,
        LocalDateTime createdAt
) {
}
