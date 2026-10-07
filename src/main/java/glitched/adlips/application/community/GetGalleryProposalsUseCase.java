package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.GalleryProposalStatus;
import java.util.List;

public class GetGalleryProposalsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final GalleryProposalPort proposalPort;
    private final UserRepositoryPort userRepository;
    private final TransactionRunner transactionRunner;

    public GetGalleryProposalsUseCase(
            GalleryProposalPort proposalPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner
    ) {
        this.proposalPort = proposalPort;
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    public GalleryProposalPage execute(Long adminUserId, String statusValue, Long cursor, int size) {
        return transactionRunner.readOnly(() -> {
            GalleryProposalAdminGuard.requireAdmin(userRepository, adminUserId);
            if ((cursor != null && cursor < 1) || size < 1 || size > MAX_PAGE_SIZE) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.VALIDATION_ERROR, "cursor와 size 값을 확인해 주세요.");
            }
            GalleryProposalStatus status = parseStatus(statusValue);

            List<GalleryProposalItem> queried = proposalPort.findPage(status, cursor, size + 1);
            boolean hasNext = queried.size() > size;
            List<GalleryProposalItem> content = hasNext ? queried.subList(0, size) : queried;
            Long nextCursor = hasNext ? content.getLast().proposalId() : null;
            return new GalleryProposalPage(List.copyOf(content), nextCursor, hasNext);
        });
    }

    private static GalleryProposalStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return GalleryProposalStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new GalleryProposalApplicationException(
                    GalleryProposalErrorCode.VALIDATION_ERROR, "올바르지 않은 개설 요청 상태입니다.");
        }
    }
}
