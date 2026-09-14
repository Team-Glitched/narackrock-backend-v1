package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
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

    default List<ProjectContribution> findByProjectIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, int pageSize) {
        return findByProjectIdAndApprovalStatusInOrderByIdDesc(projectId, statuses, PageRequest.of(0, pageSize));
    }

    default List<ProjectContribution> findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, Long cursor, int pageSize) {
        return findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
                projectId, statuses, cursor, PageRequest.of(0, pageSize));
    }

    default List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses, int pageSize) {
        return findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
                projectId, userId, statuses, PageRequest.of(0, pageSize));
    }

    default List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses,
            Long cursor, int pageSize) {
        return findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
                projectId, userId, statuses, cursor, PageRequest.of(0, pageSize));
    }
}
