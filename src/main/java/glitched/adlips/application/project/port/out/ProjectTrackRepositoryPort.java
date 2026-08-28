package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectTrack;
import java.util.List;
import java.util.Optional;

public interface ProjectTrackRepositoryPort {

    Optional<ProjectTrack> findTrackByIdAndIsDeletedFalse(Long id);

    List<ProjectTrack> findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

    List<ProjectTrack> findByProjectIdAndApprovalStatusAndIsDeletedFalse(
            Long projectId, ApprovalStatus status);

    ProjectTrack save(ProjectTrack track);
}
