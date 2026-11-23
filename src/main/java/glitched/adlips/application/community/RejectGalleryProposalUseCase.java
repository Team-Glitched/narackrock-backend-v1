package glitched.adlips.application.community;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;

public class RejectGalleryProposalUseCase {
    private final NotificationEvents notifications;

    private final GalleryProposalPort proposalPort;
    private final UserRepositoryPort userRepository;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public RejectGalleryProposalUseCase(
            GalleryProposalPort proposalPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(proposalPort, userRepository, transactionRunner, clock, NotificationEvents.none());
    }

    public RejectGalleryProposalUseCase(GalleryProposalPort proposalPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner,
            Clock clock, NotificationEvents notifications) {
        this.proposalPort = proposalPort;
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    
        this.notifications = notifications;
    }

    public GalleryProposalReviewResult execute(Long proposalId, Long adminUserId, String reason) {
        return transactionRunner.required(() -> {
            User admin = GalleryProposalAdminGuard.requireAdmin(userRepository, adminUserId);
            String sanitizedReason = glitched.adlips.domain.common.TextSanitizer.sanitizeMultiline(reason);
            if (sanitizedReason.isEmpty()) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.VALIDATION_ERROR, "반려 사유를 입력해 주세요.");
            }
            if (ContentLimits.exceeds(sanitizedReason, ContentLimits.MODERATION_REASON_MAX_LENGTH)) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.VALIDATION_ERROR,
                        "반려 사유는 " + ContentLimits.MODERATION_REASON_MAX_LENGTH + "자 이내로 입력해 주세요.");
            }
            GalleryProposal proposal = proposalPort.findByIdForUpdate(proposalId)
                    .orElseThrow(() -> new GalleryProposalApplicationException(
                            GalleryProposalErrorCode.PROPOSAL_NOT_FOUND, "존재하지 않는 갤러리 개설 요청입니다."));
            if (!proposal.isPending()) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.PROPOSAL_ALREADY_REVIEWED, "이미 검토된 갤러리 개설 요청입니다.");
            }

            proposal.reject(admin, sanitizedReason, LocalDateTime.now(clock));
            proposalPort.update(proposal);
            notifications.record(GALLERY_REJECTED,adminUserId,proposal.getId());
            return new GalleryProposalReviewResult(
                    proposal.getId(), proposal.getStatus(), null, proposal.getReviewedAt());
        });
    }
}
