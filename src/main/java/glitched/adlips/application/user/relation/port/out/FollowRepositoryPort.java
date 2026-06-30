package glitched.adlips.application.user.relation.port.out;

import java.util.List;

public interface FollowRepositoryPort {
    boolean exists(Long followerId, Long followingId);

    void save(Long followerId, Long followingId);

    boolean delete(Long followerId, Long followingId);

    List<Long> findFollowingIds(Long userId);

    List<Long> findFollowerIds(Long userId);
}
