package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.SentContributionGetListRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionGetListResponse;
import glitched.adlips.application.project.dto.response.SentContributionGetListResponse;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;

public class SentContributionGetListUseCase {
    private final ProjectContributionRepositoryPort contributions;
    private final TransactionRunner transactions;

    public SentContributionGetListUseCase(ProjectContributionRepositoryPort contributions) {
        this(contributions, TransactionRunner.direct());
    }

    public SentContributionGetListUseCase(ProjectContributionRepositoryPort contributions, TransactionRunner transactions) {
        this.contributions = contributions;
        this.transactions = transactions;
    }

    public SentContributionGetListResponse execute(SentContributionGetListRequest request) {
        if (request == null || request.userId() == null || request.userId() < 1
                || request.size() < 1 || request.size() > 100
                || (request.cursor() != null && request.cursor() < 1)) {
            throw new ProjectApplicationException(ProjectErrorCode.VALIDATION_ERROR, "조회 조건이 올바르지 않습니다.");
        }
        return transactions.readOnly(() -> {
            var fetched = contributions.findSentByUserId(request.userId(), request.cursor(), request.size() + 1);
            var items = fetched.stream().limit(request.size()).map(item -> new SentContributionGetListResponse.Item(
                    item.getId(), item.getProject().getId(), item.getProject().getTitle(),
                    item.getDescription(), item.getApprovalStatus(), item.getCreatedAt())).toList();
            return new SentContributionGetListResponse(items, new ProjectContributionGetListResponse.Page(
                    items.isEmpty() ? null : items.getLast().contributionId(), fetched.size() > request.size(), request.size()));
        });
    }
}
