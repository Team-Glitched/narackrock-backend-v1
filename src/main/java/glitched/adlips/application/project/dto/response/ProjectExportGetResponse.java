package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ExportStatus;
import java.time.LocalDateTime;

public record ProjectExportGetResponse(
        Long projectId,
        Long exportId,
        ProjectVersionResponse projectVersion,
        Long mixedAudioFileId,
        String mixedAudioUrl,
        Long layerArchiveFileId,
        String layerArchiveUrl,
        Long shortId,
        Long shortVideoFileId,
        String shortVideoUrl,
        Long albumImageFileId,
        String albumImageUrl,
        Integer durationMs,
        ExportStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
