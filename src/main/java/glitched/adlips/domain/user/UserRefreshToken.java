package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Duration;
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

    /** The token this one was rotated into; null while unused or when revoked by logout. */
    @Column(name = "replaced_by_id")
    private Long replacedById;

    protected UserRefreshToken() {
    }

    private UserRefreshToken(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            Long replacedById
    ) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revokedAt = revokedAt;
        this.replacedById = replacedById;
    }

    public static UserRefreshToken create(Long userId, String tokenHash, Instant expiresAt) {
        return new UserRefreshToken(null, userId, tokenHash, expiresAt, null, null);
    }

    public static UserRefreshToken restore(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            Long replacedById
    ) {
        return new UserRefreshToken(id, userId, tokenHash, expiresAt, revokedAt, replacedById);
    }

    public UserRefreshToken revoke(Instant revokedAt) {
        this.revokedAt = Objects.requireNonNull(revokedAt);
        return this;
    }

    /**
     * Records the rotation into {@code successorId}. The first rotation revokes this token; a retry within the
     * reuse grace keeps that time, so retries never extend the grace.
     */
    public UserRefreshToken replaceWith(Long successorId, Instant now) {
        if (revokedAt == null) {
            revokedAt = Objects.requireNonNull(now);
        }
        replacedById = Objects.requireNonNull(successorId);
        return this;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    /** Rotated (not revoked by logout) at most {@code grace} ago and not expired. */
    public boolean isRotatedWithin(Duration grace, Instant now) {
        return revokedAt != null
                && replacedById != null
                && now.isBefore(expiresAt)
                && !now.isAfter(revokedAt.plus(grace));
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

    public Long getReplacedById() {
        return replacedById;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
