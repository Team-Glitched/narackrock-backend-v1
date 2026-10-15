package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;

public class ApproveGalleryProposalUseCase {

    private final GalleryProposalPort proposalPort;
    private final GalleryManagementPort managementPort;
    private final UserRepositoryPort userRepository;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public ApproveGalleryProposalUseCase(
            GalleryProposalPort proposalPort,
            GalleryManagementPort managementPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.proposalPort = proposalPort;
        this.managementPort = managementPort;
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public GalleryProposalReviewResult execute(Long proposalId, Long adminUserId) {
        return transactionRunner.required(() -> {
            User admin = GalleryProposalAdminGuard.requireAdmin(userRepository, adminUserId);
            GalleryProposal proposal = proposalPort.findByIdForUpdate(proposalId)
                    .orElseThrow(() -> new GalleryProposalApplicationException(
                            GalleryProposalErrorCode.PROPOSAL_NOT_FOUND, "존재하지 않는 갤러리 개설 요청입니다."));
            if (!proposal.isPending()) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.PROPOSAL_ALREADY_REVIEWED, "이미 검토된 갤러리 개설 요청입니다.");
            }
            if (proposalPort.existsGalleryByName(proposal.getName())) {
                throw new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.GALLERY_NAME_DUPLICATED, "이미 존재하는 갤러리 이름입니다.");
            }

            Long galleryId = managementPort.createGallery(proposal.getName(), proposal.getDescription());
            managementPort.addManager(galleryId, proposal.getProposerId(), GalleryRole.MANAGER);
            proposal.approve(admin, LocalDateTime.now(clock));
            proposalPort.update(proposal);
            return new GalleryProposalReviewResult(
                    proposal.getId(), proposal.getStatus(), galleryId, proposal.getReviewedAt());
        });
    }
}
