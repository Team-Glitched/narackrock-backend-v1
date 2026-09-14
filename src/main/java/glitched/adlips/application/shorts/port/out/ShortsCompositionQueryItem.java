package glitched.adlips.application.shorts.port.out;

import glitched.adlips.domain.project.ProjectStatus;
import java.time.LocalDateTime;

public record ShortsCompositionQueryItem(
        Long shortId,
        Long projectId,
        String projectTitle,
        ProjectStatus projectStatus,
        LocalDateTime projectDeletedAt,
        Long mixedAudioFileId,
        String mixedAudioUrl,
        Long layerArchiveFileId,
        String layerArchiveUrl
) {
}
