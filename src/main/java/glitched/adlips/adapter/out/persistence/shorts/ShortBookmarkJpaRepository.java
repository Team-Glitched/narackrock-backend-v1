package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.domain.shorts.ShortBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShortBookmarkJpaRepository extends JpaRepository<ShortBookmark, Long> {

    boolean existsByUser_IdAndShorts_Id(Long userId, Long shortsId);

    @Modifying
    @Query(
        value = "INSERT INTO shorts_bookmarks (user_id, shorts_id, created_at) VALUES (:userId, :shortsId, CURRENT_TIMESTAMP)",
        nativeQuery = true
    )
    void insertBookmark(@Param("userId") Long userId, @Param("shortsId") Long shortsId);

    @Modifying
    @Query(
        value = "DELETE FROM shorts_bookmarks WHERE user_id = :userId AND shorts_id = :shortsId",
        nativeQuery = true
    )
    void deleteBookmark(@Param("userId") Long userId, @Param("shortsId") Long shortsId);
}
