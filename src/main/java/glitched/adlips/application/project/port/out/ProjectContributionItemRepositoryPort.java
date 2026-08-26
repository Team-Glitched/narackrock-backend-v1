package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.ProjectContributionItem;
import java.util.List;

public interface ProjectContributionItemRepositoryPort {

    boolean existsByTargetTypeAndTargetIdAndContributionApprovalStatus(
            ContributionTargetType targetType, Long targetId, ContributionApprovalStatus status);

    void saveAll(List<ProjectContributionItem> items);
}
