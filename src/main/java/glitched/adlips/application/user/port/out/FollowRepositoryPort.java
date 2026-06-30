package glitched.adlips.application.user.port.out;

public interface FollowRepositoryPort {
    boolean exists(Long followerId, Long followingId);
}
