package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.user.relation.dto.request.FollowCancelRequest;
import glitched.adlips.application.user.relation.dto.response.FollowCancelResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.user.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowCancelUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final FollowRepositoryPort followRepository;

    public FollowCancelUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            FollowRepositoryPort followRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.followRepository = followRepository;
    }

    @Transactional
    public FollowCancelResponse execute(FollowCancelRequest request) {
        Long requesterId = request.requesterId();
        Long targetUserId = request.targetUserId();
        if (requesterId != null && requesterId.equals(targetUserId)) {
            throw new UserApplicationException(UserErrorCode.CANNOT_FOLLOW_SELF, "본인을 팔로우할 수 없습니다.");
        }
        requireActiveUser(requesterId);
        requireActiveUser(targetUserId);
        if (!followRepository.delete(requesterId, targetUserId)) {
            throw new UserApplicationException(UserErrorCode.FOLLOW_NOT_FOUND, "팔로우 중인 사용자가 아닙니다.");
        }
        Profile requester = requireProfile(requesterId);
        Profile target = requireProfile(targetUserId);
        profileRepository.save(requester.decreaseFollowingCount());
        profileRepository.save(target.decreaseFollowerCount());
        return new FollowCancelResponse(
                targetUserId,
                false,
                target.getFollowerCount(),
                requester.getFollowingCount()
        );
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
