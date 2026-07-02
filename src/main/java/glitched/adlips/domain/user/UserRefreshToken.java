package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "user_refresh_tokens",
        indexes = {
                @Index(columnList = "user_id"),
                @Index(columnList = "expires_at")
        }
)
public class UserRefreshToken extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected UserRefreshToken() {
    }

    private UserRefreshToken(Long id, Long userId, String tokenHash, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revokedAt = revokedAt;
    }

    public static UserRefreshToken create(Long userId, String tokenHash, Instant expiresAt) {
        return new UserRefreshToken(null, userId, tokenHash, expiresAt, null);
    }

    public static UserRefreshToken restore(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt
    ) {
        return new UserRefreshToken(id, userId, tokenHash, expiresAt, revokedAt);
    }

    public UserRefreshToken revoke(Instant revokedAt) {
        this.revokedAt = Objects.requireNonNull(revokedAt);
        return this;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
