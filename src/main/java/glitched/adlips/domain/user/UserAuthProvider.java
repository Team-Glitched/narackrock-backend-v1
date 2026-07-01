package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(
        name = "user_auth_providers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(columnNames = {"user_id", "provider"})
        },
        indexes = @Index(columnList = "user_id")
)
public class UserAuthProvider extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    protected UserAuthProvider() {
    }

    private UserAuthProvider(
            Long id,
            Long userId,
            AuthProvider provider,
            String providerUserId,
            boolean emailVerified
    ) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId);
        this.provider = Objects.requireNonNull(provider);
        this.providerUserId = requireText(providerUserId);
        this.emailVerified = emailVerified;
    }

    public static UserAuthProvider google(Long userId, String providerUserId, boolean emailVerified) {
        return new UserAuthProvider(null, userId, AuthProvider.GOOGLE, providerUserId, emailVerified);
    }

    public static UserAuthProvider restore(
            Long id,
            Long userId,
            AuthProvider provider,
            String providerUserId,
            boolean emailVerified
    ) {
        return new UserAuthProvider(id, userId, provider, providerUserId, emailVerified);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("인증 제공자 사용자 식별자는 필수입니다.");
        }
        return value;
    }
}
