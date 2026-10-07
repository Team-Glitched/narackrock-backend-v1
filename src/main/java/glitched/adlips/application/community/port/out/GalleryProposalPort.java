package glitched.adlips.application.community.port.out;

import java.util.List;

public interface GalleryProposalPort {
    boolean existsGalleryByName(String name);

    boolean existsPendingByName(String name);

    long countPendingByProposer(Long userId);

    Long save(Long userId, String name, String description);

    List<GalleryProposalItem> findByProposer(Long userId);
}
