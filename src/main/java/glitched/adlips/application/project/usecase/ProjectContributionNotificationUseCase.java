package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.response.ProjectContributionNotificationResponse;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.project.ContributionApprovalStatus;

public class ProjectContributionNotificationUseCase {
    private final ProjectContributionRepositoryPort contributions;
    private final ProfileRepositoryPort profiles;
    private final TransactionRunner transactionRunner;

    public ProjectContributionNotificationUseCase(
            ProjectContributionRepositoryPort contributions,
            ProfileRepositoryPort profiles) {
        this(contributions, profiles, TransactionRunner.direct());
    }

    public ProjectContributionNotificationUseCase(
            ProjectContributionRepositoryPort contributions,
            ProfileRepositoryPort profiles,
            TransactionRunner transactionRunner) {
        this.contributions = contributions;
        this.profiles = profiles;
        this.transactionRunner = transactionRunner;
    }

    public ProjectContributionNotificationResponse execute(Long ownerId) {
        return transactionRunner.readOnly(() -> new ProjectContributionNotificationResponse(
                contributions.findByProjectOwnerIdAndApprovalStatusOrderByIdDesc(
                                ownerId, ContributionApprovalStatus.PENDING)
                        .stream()
                        .map(contribution -> {
                            Long contributorId = contribution.getUser().getId();
                            String nickname = profiles.findByUserId(contributorId)
                                    .map(profile -> profile.getNickname())
                                    .orElse("알 수 없는 사용자");
                            return new ProjectContributionNotificationResponse.Item(
                                    contribution.getId(),
                                    contribution.getProject().getId(),
                                    contribution.getProject().getTitle(),
                                    contributorId,
                                    nickname,
                                    contribution.getDescription(),
                                    contribution.getItems().size(),
                                    contribution.getCreatedAt());
                        })
                        .toList()));
    }
}
