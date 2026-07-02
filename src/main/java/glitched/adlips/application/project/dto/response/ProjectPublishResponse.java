package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ExportStatus;

public record ProjectPublishResponse(Long projectId, Long exportId, ProjectVersionResponse projectVersion,
                                     Long mixedAudioFileId, Long shortId, ExportStatus exportStatus) {
}
