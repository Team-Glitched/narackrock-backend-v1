package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ProjectContribution;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectContributionRepositoryPort {

    Optional<ProjectContribution> findByIdAndProjectId(Long id, Long projectId);

    List<ProjectContribution> findByProjectIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, int pageSize);

    List<ProjectContribution> findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, Long cursor, int pageSize);

    List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses, int pageSize);

    List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses,
            Long cursor, int pageSize);

    ProjectContribution save(ProjectContribution contribution);
}
