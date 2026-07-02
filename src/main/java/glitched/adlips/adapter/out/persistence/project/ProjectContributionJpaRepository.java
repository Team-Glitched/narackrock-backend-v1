package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectContributionJpaRepository extends JpaRepository<ProjectContribution, Long> {
    Optional<ProjectContribution> findByIdAndProjectId(Long id, Long projectId);

    List<ProjectContribution> findByProjectIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, Pageable pageable);

    List<ProjectContribution> findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, Long cursor, Pageable pageable);

    List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses, Pageable pageable);

    List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses,
            Long cursor, Pageable pageable);
}
