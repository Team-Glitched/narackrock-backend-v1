package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.port.out.FollowRepositoryPort;
import glitched.adlips.domain.user.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataFollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
}

@Repository
public class FollowQueryPersistenceAdapter implements FollowRepositoryPort {
    private final SpringDataFollowRepository followRepository;

    public FollowQueryPersistenceAdapter(SpringDataFollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    public boolean exists(Long followerId, Long followingId) {
        return followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId);
    }
}
