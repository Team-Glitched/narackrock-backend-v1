package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectClip;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectClipJpaRepository extends JpaRepository<ProjectClip, Long> {
    Optional<ProjectClip> findByIdAndIsDeletedFalse(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select clip from ProjectClip clip where clip.id = :id and clip.isDeleted = false")
    Optional<ProjectClip> findActiveForUpdate(@Param("id") Long id);

    List<ProjectClip> findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(Long trackId);
    long countByProjectIdAndApprovalStatusAndIsDeletedFalse(Long projectId, ApprovalStatus status);
    long countByTrackIdInAndApprovalStatusAndIsDeletedFalse(List<Long> trackIds, ApprovalStatus status);
    List<ProjectClip> findByTrackIdAndApprovalStatusAndIsDeletedFalseOrderByStartTickAscIdAsc(
            Long trackId, ApprovalStatus status);
}
