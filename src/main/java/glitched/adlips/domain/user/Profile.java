package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseUpdatedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "profiles", indexes = @Index(columnList = "nickname"))
public class Profile extends BaseUpdatedEntity {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image_file_id")
    private Long profileImageFileId;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "primary_instrument")
    private String primaryInstrument;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    @Column(name = "following_count", nullable = false)
    private int followingCount = 0;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    protected Profile() {
    }

    public Profile(User user, String nickname) {
        this.user = user;
        this.nickname = nickname;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
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
        return isPrivate;
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
}
