package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "profiles", indexes = @Index(columnList = "nickname"))
public class Profile extends BaseUpdatedEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image_file_id")
    private Long profileImageFileId;

    private String explanation;

    @Column(name = "primary_instrument")
    private String primaryInstrument;

    @Column(name = "is_private", nullable = false)
    private boolean privateProfile;

    @Column(name = "follower_count", nullable = false)
    private int followerCount;

    @Column(name = "following_count", nullable = false)
    private int followingCount;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    protected Profile() {
    }

    private Profile(
            Long userId,
            String nickname,
            Long profileImageFileId,
            String explanation,
            String primaryInstrument,
            boolean privateProfile,
            int followerCount,
            int followingCount,
            LocalDateTime nicknameUpdatedAt
    ) {
        this.userId = Objects.requireNonNull(userId, "사용자 식별자는 필수입니다.");
        this.nickname = requireNickname(nickname);
        this.profileImageFileId = profileImageFileId;
        this.explanation = explanation;
        this.primaryInstrument = primaryInstrument;
        this.privateProfile = privateProfile;
        this.followerCount = requireNonNegative(followerCount);
        this.followingCount = requireNonNegative(followingCount);
        this.nicknameUpdatedAt = nicknameUpdatedAt;
    }

    public static Profile create(Long userId, String nickname) {
        return new Profile(userId, nickname, null, null, null, false, 0, 0, null);
    }

    public static Profile restore(
            Long userId,
            String nickname,
            Long profileImageFileId,
            String explanation,
            String primaryInstrument,
            boolean privateProfile,
            int followerCount,
            int followingCount,
            LocalDateTime nicknameUpdatedAt
    ) {
        return new Profile(
                userId, nickname, profileImageFileId, explanation, primaryInstrument,
                privateProfile, followerCount, followingCount, nicknameUpdatedAt
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public Long getProfileImageFileId() {
        return profileImageFileId;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getPrimaryInstrument() {
        return primaryInstrument;
    }

    public boolean isPrivate() {
        return privateProfile;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public LocalDateTime getNicknameUpdatedAt() {
        return nicknameUpdatedAt;
    }

    public Profile update(
            String nickname,
            String primaryInstrument,
            String explanation,
            LocalDateTime changedAt
    ) {
        if (nickname != null && !this.nickname.equals(nickname.trim())) {
            this.nickname = requireNickname(nickname);
            this.nicknameUpdatedAt = Objects.requireNonNull(changedAt);
        }
        if (primaryInstrument != null) {
            this.primaryInstrument = normalizeOptional(primaryInstrument);
        }
        if (explanation != null) {
            this.explanation = normalizeOptional(explanation);
        }
        return this;
    }

    public Profile changeProfileImage(Long mediaFileId) {
        this.profileImageFileId = Objects.requireNonNull(mediaFileId);
        return this;
    }

    private static String requireNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        return nickname.trim();
    }

    private static int requireNonNegative(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("관계 수는 음수일 수 없습니다.");
        }
        return count;
    }

    private static String normalizeOptional(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
