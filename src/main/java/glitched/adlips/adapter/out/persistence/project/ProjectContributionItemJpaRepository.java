package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.ProjectContributionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectContributionItemJpaRepository extends JpaRepository<ProjectContributionItem, Long> {
    boolean existsByTargetTypeAndTargetIdAndContributionApprovalStatus(
            ContributionTargetType targetType, Long targetId, ContributionApprovalStatus status);
}
