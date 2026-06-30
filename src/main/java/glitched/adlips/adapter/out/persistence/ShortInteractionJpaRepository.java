package glitched.adlips.adapter.out.persistence;

import glitched.adlips.domain.shorts.ShortBookmark;
import glitched.adlips.domain.shorts.ShortDislike;
import glitched.adlips.domain.shorts.ShortLike;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortLikeJpaRepository extends JpaRepository<ShortLike, Long> {
    @Query("SELECT l.shorts.id FROM ShortLike l WHERE l.user.id = :userId AND l.shorts.id IN :shortIds")
    Set<Long> findShortIdsByUserIdAndShortIdIn(@Param("userId") Long userId, @Param("shortIds") List<Long> shortIds);
}

interface ShortDislikeJpaRepository extends JpaRepository<ShortDislike, Long> {
    @Query("SELECT d.shorts.id FROM ShortDislike d WHERE d.user.id = :userId AND d.shorts.id IN :shortIds")
    Set<Long> findShortIdsByUserIdAndShortIdIn(@Param("userId") Long userId, @Param("shortIds") List<Long> shortIds);
}

interface ShortBookmarkJpaRepository extends JpaRepository<ShortBookmark, Long> {
    @Query("SELECT b.shorts.id FROM ShortBookmark b WHERE b.user.id = :userId AND b.shorts.id IN :shortIds")
    Set<Long> findShortIdsByUserIdAndShortIdIn(@Param("userId") Long userId, @Param("shortIds") List<Long> shortIds);
}
