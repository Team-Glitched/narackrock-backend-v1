package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import java.util.List;
import java.util.Optional;

public interface GalleryProposalPort {
    boolean existsGalleryByName(String name);

    boolean existsPendingByName(String name);

    long countPendingByProposer(Long userId);

    Long save(Long userId, String name, String description);

    List<GalleryProposalItem> findByProposer(Long userId);

    Optional<GalleryProposal> findByIdForUpdate(Long proposalId);

    void update(GalleryProposal proposal);

    /** status가 null이면 전체, cursor가 null이면 처음부터. id 내림차순. */
    List<GalleryProposalItem> findPage(GalleryProposalStatus status, Long cursor, int limit);
}
