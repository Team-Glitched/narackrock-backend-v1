package glitched.adlips.adapter.out.persistence.admin;

import glitched.adlips.domain.admin.UserBan;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserBanJpaRepository extends JpaRepository<UserBan, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM UserBan b
            WHERE b.user.id = :userId
              AND b.liftedAt IS NULL
              AND (b.bannedUntil IS NULL OR b.bannedUntil > :now)
            """)
    boolean existsActiveBan(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
