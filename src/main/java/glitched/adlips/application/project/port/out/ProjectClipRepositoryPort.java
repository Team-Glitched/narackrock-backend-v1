package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectClip;
import java.util.List;
import java.util.Optional;

public interface ProjectClipRepositoryPort {

    Optional<ProjectClip> findClipByIdAndIsDeletedFalse(Long id);

    List<ProjectClip> findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(Long trackId);

    long countByTrackIdInAndApprovalStatusAndIsDeletedFalse(
            List<Long> trackIds, ApprovalStatus status);

    ProjectClip save(ProjectClip clip);
}
