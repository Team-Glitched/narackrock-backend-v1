package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.domain.community.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostBookmarkJpaRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);

    @Modifying
    @Query(
        value = "INSERT INTO post_bookmarks (user_id, post_id, created_at) VALUES (:userId, :postId, CURRENT_TIMESTAMP)",
        nativeQuery = true
    )
    void insertBookmark(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying
    @Query(
        value = "DELETE FROM post_bookmarks WHERE user_id = :userId AND post_id = :postId",
        nativeQuery = true
    )
    void deleteBookmark(@Param("userId") Long userId, @Param("postId") Long postId);
}
