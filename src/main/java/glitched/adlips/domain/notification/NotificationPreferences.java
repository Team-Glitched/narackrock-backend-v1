package glitched.adlips.domain.notification;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {
    public record Values(
            boolean reactions,
            boolean followingContent,
            boolean communityPosts,
            boolean sourceUsage) {
        public static Values defaults() {
            return new Values(true, false, false, false);
        }

        public boolean allows(NotificationType type) {
            return (!type.reaction() || reactions)
                    && (!type.followingContent() || followingContent)
                    && (!type.communityPosts() || communityPosts)
                    && (!type.sourceUsage() || sourceUsage);
        }
    }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private boolean reactions = true;

    @Column(name = "following_content", nullable = false)
    private boolean followingContent;

    @Column(name = "community_posts", nullable = false)
    private boolean communityPosts;

    @Column(name = "source_usage", nullable = false)
    private boolean sourceUsage;

    protected NotificationPreferences() {}

    public NotificationPreferences(Long userId, Values values) {
        this.userId = userId;
        set(values);
    }

    public Values values() {
        return new Values(reactions, followingContent, communityPosts, sourceUsage);
    }

    public void set(Values v) {
        reactions = v.reactions();
        followingContent = v.followingContent();
        communityPosts = v.communityPosts();
        sourceUsage = v.sourceUsage();
    }
}
