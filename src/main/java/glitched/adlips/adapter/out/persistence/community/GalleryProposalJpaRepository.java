package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface GalleryProposalJpaRepository extends JpaRepository<GalleryProposal, Long> {

    boolean existsByNameAndStatus(String name, GalleryProposalStatus status);

    long countByProposer_IdAndStatus(Long userId, GalleryProposalStatus status);

    List<GalleryProposal> findByProposer_IdOrderByIdDesc(Long userId);
}
