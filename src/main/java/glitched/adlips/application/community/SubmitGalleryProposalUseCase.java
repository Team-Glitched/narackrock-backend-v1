package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Gallery;
import java.time.Clock;
import java.time.LocalDateTime;

public class SubmitGalleryProposalUseCase {

    static final long MAX_PENDING_PROPOSALS = 1;

    private final GalleryProposalPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public SubmitGalleryProposalUseCase(
            GalleryProposalPort port,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.port = port;
        this.userBanQueryPort = userBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public Long execute(Long userId, String name, String description) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new GalleryProposalApplicationException(
                    GalleryProposalErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 갤러리 개설을 요청할 수 없습니다.");
        }
        String normalizedName;
        String normalizedDescription;
        try {
            normalizedName = Gallery.normalizeName(name);
            normalizedDescription = Gallery.normalizeDescription(description);
        } catch (IllegalArgumentException e) {
            throw new GalleryProposalApplicationException(GalleryProposalErrorCode.VALIDATION_ERROR, e.getMessage());
        }

        return transactionRunner.required(() -> {
            if (port.existsGalleryByName(normalizedName)) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.GALLERY_NAME_DUPLICATED, "이미 존재하는 갤러리 이름입니다.");
            }
            if (port.existsPendingByName(normalizedName)) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.PROPOSAL_NAME_DUPLICATED, "같은 이름으로 검토 중인 개설 요청이 있습니다.");
            }
            if (port.countPendingByProposer(userId) >= MAX_PENDING_PROPOSALS) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.TOO_MANY_PENDING_PROPOSALS, "검토 중인 개설 요청이 이미 있습니다.");
            }
            return port.save(userId, normalizedName, normalizedDescription);
        });
    }
}
