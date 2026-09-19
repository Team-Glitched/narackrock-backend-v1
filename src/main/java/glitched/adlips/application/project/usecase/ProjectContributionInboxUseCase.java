package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.response.ProjectContributionInboxResponse;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;

public class ProjectContributionInboxUseCase {
    private final ProjectContributionRepositoryPort contributions;
    private final ProfileRepositoryPort profiles;
    private final TransactionRunner transactions;

    public ProjectContributionInboxUseCase(ProjectContributionRepositoryPort contributions,
                                           ProfileRepositoryPort profiles) {
        this(contributions, profiles, TransactionRunner.direct());
    }

    public ProjectContributionInboxUseCase(ProjectContributionRepositoryPort contributions,
                                           ProfileRepositoryPort profiles, TransactionRunner transactions) {
        this.contributions = contributions;
        this.profiles = profiles;
        this.transactions = transactions;
    }

    public ProjectContributionInboxResponse execute(Long ownerId) {
        if (ownerId == null || ownerId < 1) {
            throw new ProjectApplicationException(ProjectErrorCode.VALIDATION_ERROR, "사용자 정보가 올바르지 않습니다.");
        }
        return transactions.readOnly(() -> new ProjectContributionInboxResponse(
                contributions.findPendingForOwner(ownerId, 50).stream().map(contribution -> {
                    var project = contribution.getProject();
                    var contributor = contribution.getUser();
                    String nickname = profiles.findByUserId(contributor.getId())
                            .map(profile -> profile.getNickname()).orElse("사용자");
                    return new ProjectContributionInboxResponse.Item(
                            contribution.getId(), project.getId(), project.getTitle(),
                            contributor.getId(), nickname, contribution.getDescription(),
                            contribution.getItems().size(), contribution.getCreatedAt());
                }).toList()));
    }
}
