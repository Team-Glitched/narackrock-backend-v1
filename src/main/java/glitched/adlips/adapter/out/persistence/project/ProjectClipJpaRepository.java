package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectClip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectClipJpaRepository extends JpaRepository<ProjectClip, Long> {
    Optional<ProjectClip> findByIdAndIsDeletedFalse(Long id);
    List<ProjectClip> findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(Long trackId);
    long countByProjectIdAndApprovalStatusAndIsDeletedFalse(Long projectId, ApprovalStatus status);
    long countByTrackIdInAndApprovalStatusAndIsDeletedFalse(List<Long> trackIds, ApprovalStatus status);
}
