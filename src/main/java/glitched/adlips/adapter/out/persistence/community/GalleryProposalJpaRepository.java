package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.GalleryProposal;
import glitched.adlips.domain.community.GalleryProposalStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GalleryProposalJpaRepository extends JpaRepository<GalleryProposal, Long> {

    boolean existsByNameAndStatus(String name, GalleryProposalStatus status);

    long countByProposer_IdAndStatus(Long userId, GalleryProposalStatus status);

    List<GalleryProposal> findByProposer_IdOrderByIdDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM GalleryProposal p WHERE p.id = :proposalId")
    Optional<GalleryProposal> findByIdForUpdate(@Param("proposalId") Long proposalId);

    @Query("""
            SELECT p FROM GalleryProposal p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
            """)
    List<GalleryProposal> findPage(
            @Param("status") GalleryProposalStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
