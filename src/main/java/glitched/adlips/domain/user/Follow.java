package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "follows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}),
        indexes = {
                @Index(columnList = "follower_id"),
                @Index(columnList = "following_id")
        }
)
public class Follow extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    protected Follow() {
    }

    private Follow(User follower, User following) {
        if (follower.getId().equals(following.getId())) {
            throw new IllegalArgumentException("본인을 팔로우할 수 없습니다.");
        }
        this.follower = follower;
        this.following = following;
    }

    public static Follow create(User follower, User following) {
        return new Follow(follower, following);
    }

    public Long getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowing() {
        return following;
    }
}
