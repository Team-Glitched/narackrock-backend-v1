package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectTrack;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectTrackJpaRepository extends JpaRepository<ProjectTrack, Long> {
    Optional<ProjectTrack> findByIdAndIsDeletedFalse(Long id);
    @Query("SELECT t.project.id FROM ProjectTrack t WHERE t.id = :id AND t.isDeleted = false")
    Optional<Long> findProjectIdByIdAndIsDeletedFalse(@Param("id") Long id);
    List<ProjectTrack> findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);
    List<ProjectTrack> findByProjectIdAndApprovalStatusAndIsDeletedFalse(Long projectId, ApprovalStatus status);
    boolean existsByProjectIdAndOwnerIdAndIsDeletedFalse(Long projectId, Long ownerId);
    boolean existsByProjectIdAndOwnerIdAndOriginOpenSourceIdIsNullAndIsDeletedFalse(Long projectId, Long ownerId);
}
