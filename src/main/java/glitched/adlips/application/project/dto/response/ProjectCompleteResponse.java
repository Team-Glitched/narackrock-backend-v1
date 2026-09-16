package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ProjectStatus;

public record ProjectCompleteResponse(Long projectId, ProjectStatus status, boolean isPublic) {
}
