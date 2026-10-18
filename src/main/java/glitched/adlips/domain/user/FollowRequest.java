package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "follow_requests", uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "target_id"}), indexes = @Index(columnList = "target_id"))
public class FollowRequest extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    protected FollowRequest() {}

    public FollowRequest(Long requesterId, Long targetId) {
        if (requesterId == null || targetId == null || requesterId.equals(targetId)) throw new IllegalArgumentException("올바르지 않은 팔로우 요청입니다.");
        this.requesterId = requesterId;
        this.targetId = targetId;
    }
    public Long getId() { return id; }
    public Long getRequesterId() { return requesterId; }
    public Long getTargetId() { return targetId; }
}
