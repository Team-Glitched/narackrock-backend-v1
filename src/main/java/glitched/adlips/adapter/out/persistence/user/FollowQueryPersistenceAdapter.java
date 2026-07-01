package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.user.Follow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataFollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    long deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    List<Follow> findAllByFollower_Id(Long followerId);

    List<Follow> findAllByFollowing_Id(Long followingId);
}

@Repository
public class FollowQueryPersistenceAdapter implements FollowRepositoryPort {
    private final SpringDataFollowRepository followRepository;
    private final SpringDataUserRepository userRepository;

    public FollowQueryPersistenceAdapter(
            SpringDataFollowRepository followRepository,
            SpringDataUserRepository userRepository
    ) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean exists(Long followerId, Long followingId) {
        return followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId);
    }

    @Override
    public void save(Long followerId, Long followingId) {
        followRepository.save(Follow.create(
                userRepository.getReferenceById(followerId),
                userRepository.getReferenceById(followingId)
        ));
    }

    @Override
    public boolean delete(Long followerId, Long followingId) {
        return followRepository.deleteByFollower_IdAndFollowing_Id(followerId, followingId) > 0;
    }

    @Override
    public List<Long> findFollowingIds(Long userId) {
        return followRepository.findAllByFollower_Id(userId).stream()
                .map(follow -> follow.getFollowing().getId())
                .toList();
    }

    @Override
    public List<Long> findFollowerIds(Long userId) {
        return followRepository.findAllByFollowing_Id(userId).stream()
                .map(follow -> follow.getFollower().getId())
                .toList();
    }
}
