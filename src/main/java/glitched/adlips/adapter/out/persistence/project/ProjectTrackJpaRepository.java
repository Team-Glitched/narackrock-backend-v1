package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectTrack;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTrackJpaRepository extends JpaRepository<ProjectTrack, Long> {
    Optional<ProjectTrack> findByIdAndIsDeletedFalse(Long id);
    List<ProjectTrack> findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);
    List<ProjectTrack> findByProjectIdAndApprovalStatusAndIsDeletedFalse(Long projectId, ApprovalStatus status);
}
