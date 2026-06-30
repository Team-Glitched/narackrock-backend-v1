package glitched.adlips.application.user.relation;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.user.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowCreateUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final FollowRepositoryPort followRepository;

    public FollowCreateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            FollowRepositoryPort followRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.followRepository = followRepository;
    }

    @Transactional
    public FollowResult execute(Long requesterId, Long targetUserId) {
        validateDifferentUsers(requesterId, targetUserId);
        requireActiveUser(requesterId);
        requireActiveUser(targetUserId);
        if (followRepository.exists(requesterId, targetUserId)) {
            throw new UserApplicationException(UserErrorCode.ALREADY_FOLLOWING, "이미 팔로우 중인 사용자입니다.");
        }
        Profile requester = requireProfile(requesterId);
        Profile target = requireProfile(targetUserId);
        followRepository.save(requesterId, targetUserId);
        profileRepository.save(requester.increaseFollowingCount());
        profileRepository.save(target.increaseFollowerCount());
        return new FollowResult(
                targetUserId,
                true,
                target.getFollowerCount(),
                requester.getFollowingCount()
        );
    }

    private void validateDifferentUsers(Long requesterId, Long targetUserId) {
        if (requesterId != null && requesterId.equals(targetUserId)) {
            throw new UserApplicationException(UserErrorCode.CANNOT_FOLLOW_SELF, "본인을 팔로우할 수 없습니다.");
        }
    }

    private void requireActiveUser(Long userId) {
        userRepository.findById(userId)
                .filter(user -> user.isActive())
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
    }

    private Profile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.PROFILE_NOT_FOUND,
                        "존재하지 않는 사용자 프로필입니다."
                ));
    }
}
