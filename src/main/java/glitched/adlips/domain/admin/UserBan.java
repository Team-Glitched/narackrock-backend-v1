package glitched.adlips.domain.admin;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_bans", indexes = {
        @Index(name = "idx_user_bans_user_lifted", columnList = "user_id, lifted_at"),
        @Index(name = "idx_user_bans_admin", columnList = "admin_id")
})
public class UserBan extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "admin_id", nullable = false) private User admin;
    @Column(name = "reason", nullable = false) private String reason;
    @Column(name = "banned_until") private LocalDateTime bannedUntil;
    @Column(name = "lifted_at") private LocalDateTime liftedAt;
}
