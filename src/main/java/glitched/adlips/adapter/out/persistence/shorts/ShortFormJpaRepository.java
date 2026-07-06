package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortFormJpaRepository extends JpaRepository<ShortForm, Long> {

    @Query("""
            SELECT new glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem(
                s.id, p.id, p.title, p.status, p.deletedAt)
            FROM ShortForm s
            LEFT JOIN s.project p
            WHERE s.id = :shortId
              AND s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
              AND s.deletedAt IS NULL
            """)
    Optional<ShortsCompositionQueryItem> findCompositionInfo(@Param("shortId") Long shortId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ShortForm s
            WHERE s.id = :shortId
              AND s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
              AND s.deletedAt IS NULL
            """)
    Optional<ShortForm> findActiveByIdForUpdate(@Param("shortId") Long shortId);

    @Modifying
    @Query("UPDATE ShortForm s SET s.likeCount = s.likeCount + :delta WHERE s.id = :shortId")
    void adjustLikeCount(@Param("shortId") Long shortId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE ShortForm s SET s.dislikeCount = s.dislikeCount + :delta WHERE s.id = :shortId")
    void adjustDislikeCount(@Param("shortId") Long shortId, @Param("delta") int delta);

    @Query("SELECT s.likeCount FROM ShortForm s WHERE s.id = :shortId")
    Integer getLikeCount(@Param("shortId") Long shortId);

    @Query("SELECT s.dislikeCount FROM ShortForm s WHERE s.id = :shortId")
    Integer getDislikeCount(@Param("shortId") Long shortId);

    boolean existsByIdAndStatusAndDeletedAtIsNull(Long id, ShortStatus status);
}
