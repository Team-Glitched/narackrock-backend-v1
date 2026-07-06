package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.usecase.ProjectMemberJoinUseCase;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;

public class ShortsCompositionEntryUseCase {

    private final ShortsCompositionQueryPort queryPort;
    private final ProjectMemberJoinUseCase projectMemberJoinUseCase;
    private final TransactionRunner transactionRunner;

    public ShortsCompositionEntryUseCase(
            ShortsCompositionQueryPort queryPort,
            ProjectMemberJoinUseCase projectMemberJoinUseCase,
            TransactionRunner transactionRunner
    ) {
        this.queryPort = queryPort;
        this.projectMemberJoinUseCase = projectMemberJoinUseCase;
        this.transactionRunner = transactionRunner;
    }

    public ShortsCompositionQueryItem execute(Long shortId, Long userId) {
        return transactionRunner.required(() -> enter(shortId, userId));
    }

    private ShortsCompositionQueryItem enter(Long shortId, Long userId) {
        ShortsCompositionQueryItem item = queryPort.findByShortId(shortId)
                .orElseThrow(() -> new ShortsCompositionApplicationException(
                        ShortsCompositionErrorCode.SHORT_NOT_FOUND, "숏폼을 찾을 수 없습니다."));

        if (item.projectId() == null || item.projectDeletedAt() != null) {
            throw new ShortsCompositionApplicationException(
                    ShortsCompositionErrorCode.PROJECT_NOT_LINKED, "연결된 작곡 프로젝트를 찾을 수 없습니다.");
        }

        projectMemberJoinUseCase.execute(item.projectId(), userId);
        return item;
    }
}
