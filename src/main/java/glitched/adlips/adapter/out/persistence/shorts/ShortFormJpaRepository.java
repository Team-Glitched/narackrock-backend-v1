package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.domain.shorts.ShortForm;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortFormJpaRepository extends JpaRepository<ShortForm, Long> {

    @Query("""
            SELECT new glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem(
                s.id, p.id, p.title, p.status, p.deletedAt,
                export.mediaFileId, mixedAudio.fileUrl,
                export.layerArchiveFileId, layerArchive.fileUrl)
            FROM ShortForm s
            LEFT JOIN s.project p
            LEFT JOIN ProjectExport export ON export.id = s.exportId
            LEFT JOIN MediaFileJpaEntity mixedAudio ON mixedAudio.id = export.mediaFileId
            LEFT JOIN MediaFileJpaEntity layerArchive ON layerArchive.id = export.layerArchiveFileId
            WHERE s.id = :shortId
              AND (s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
                   OR (s.status = glitched.adlips.domain.shorts.ShortStatus.IN_PROGRESS
                       AND p.isPublic = true))
              AND s.deletedAt IS NULL
            """)
    Optional<ShortsCompositionQueryItem> findCompositionInfo(@Param("shortId") Long shortId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ShortForm s
            LEFT JOIN s.project p
            WHERE s.id = :shortId
              AND (s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
                   OR (s.status = glitched.adlips.domain.shorts.ShortStatus.IN_PROGRESS
                       AND p.isPublic = true))
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

    @Query("""
            SELECT COUNT(s) > 0 FROM ShortForm s
            LEFT JOIN s.project p
            WHERE s.id = :shortId AND s.deletedAt IS NULL
              AND (s.status = glitched.adlips.domain.shorts.ShortStatus.COMPLETED
                   OR (s.status = glitched.adlips.domain.shorts.ShortStatus.IN_PROGRESS
                       AND p.isPublic = true))
            """)
    boolean existsPlayableById(@Param("shortId") Long shortId);

    List<ShortForm> findByProjectIdAndDeletedAtIsNull(Long projectId);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT s.user.id FROM ShortForm s WHERE s.id = :shortId AND s.deletedAt IS NULL")
    Optional<Long> findActiveOwnerId(@Param("shortId") Long shortId);

    @Query("SELECT s.title FROM ShortForm s WHERE s.id = :shortId AND s.deletedAt IS NULL")
    Optional<String> findActiveTitle(@Param("shortId") Long shortId);

    @Modifying
    @Query("UPDATE ShortForm s SET s.commentCount = s.commentCount + :delta WHERE s.id = :shortId")
    void adjustCommentCount(@Param("shortId") Long shortId, @Param("delta") int delta);
}
