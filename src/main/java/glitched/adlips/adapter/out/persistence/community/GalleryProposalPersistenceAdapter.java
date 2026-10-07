package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryProposalItem;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryProposalPersistenceAdapter implements GalleryProposalPort {

    private final GalleryProposalJpaRepository proposalRepository;
    private final GalleryJpaRepository galleryRepository;
    private final EntityManager entityManager;

    public GalleryProposalPersistenceAdapter(
            GalleryProposalJpaRepository proposalRepository,
            GalleryJpaRepository galleryRepository,
            EntityManager entityManager
    ) {
        this.proposalRepository = proposalRepository;
        this.galleryRepository = galleryRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsGalleryByName(String name) {
        return galleryRepository.existsByName(name);
    }

    @Override
    public boolean existsPendingByName(String name) {
        return proposalRepository.existsByNameAndStatus(name, GalleryProposalStatus.PENDING);
    }

    @Override
    public long countPendingByProposer(Long userId) {
        return proposalRepository.countByProposer_IdAndStatus(userId, GalleryProposalStatus.PENDING);
    }

    @Override
    public Long save(Long userId, String name, String description) {
        User proposer = entityManager.getReference(User.class, userId);
        return proposalRepository.save(GalleryProposal.submit(proposer, name, description)).getId();
    }

    @Override
    public List<GalleryProposalItem> findByProposer(Long userId) {
        return proposalRepository.findByProposer_IdOrderByIdDesc(userId).stream()
                .map(GalleryProposalPersistenceAdapter::toItem)
                .toList();
    }

    static GalleryProposalItem toItem(GalleryProposal proposal) {
        return new GalleryProposalItem(
                proposal.getId(),
                proposal.getProposerId(),
                proposal.getName(),
                proposal.getDescription(),
                proposal.getStatus(),
                proposal.getRejectionReason(),
                proposal.getCreatedAt(),
                proposal.getReviewedAt());
    }
}
