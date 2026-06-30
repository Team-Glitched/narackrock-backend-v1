package glitched.adlips.application.user.relation;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowService {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final ProfileQueryPort profileQuery;
    private final FollowRepositoryPort followRepository;
    private final MediaFileRepositoryPort mediaFileRepository;

    public FollowService(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.profileQuery = profileQuery;
        this.followRepository = followRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    @Transactional
    public FollowResult follow(Long requesterId, Long targetUserId) {
        validateDifferentUsers(requesterId, targetUserId);
        requireActiveUser(requesterId);
        requireActiveUser(targetUserId);
        if (followRepository.exists(requesterId, targetUserId)) {
            throw new UserApplicationException(
                    UserErrorCode.ALREADY_FOLLOWING,
                    "이미 팔로우 중인 사용자입니다."
            );
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

    @Transactional
    public FollowResult unfollow(Long requesterId, Long targetUserId) {
        validateDifferentUsers(requesterId, targetUserId);
        requireActiveUser(requesterId);
        requireActiveUser(targetUserId);
        if (!followRepository.delete(requesterId, targetUserId)) {
            throw new UserApplicationException(
                    UserErrorCode.FOLLOW_NOT_FOUND,
                    "팔로우 중인 사용자가 아닙니다."
            );
        }
        Profile requester = requireProfile(requesterId);
        Profile target = requireProfile(targetUserId);
        profileRepository.save(requester.decreaseFollowingCount());
        profileRepository.save(target.decreaseFollowerCount());
        return new FollowResult(
                targetUserId,
                false,
                target.getFollowerCount(),
                requester.getFollowingCount()
        );
    }

    public List<UserCard> getRelations(Long userId, RelationType type) {
        requireActiveUser(userId);
        List<Long> relatedUserIds = type == RelationType.FOLLOWING
                ? followRepository.findFollowingIds(userId)
                : followRepository.findFollowerIds(userId);
        return profileQuery.findAllByUserIds(relatedUserIds).stream()
                .map(this::toUserCard)
                .toList();
    }

    private UserCard toUserCard(Profile profile) {
        String profileImageUrl = profile.getProfileImageFileId() == null
                ? null
                : mediaFileRepository.findById(profile.getProfileImageFileId())
                        .map(MediaFile::getFileUrl)
                        .orElse(null);
        return new UserCard(
                profile.getUserId(),
                profile.getNickname(),
                profileImageUrl,
                profile.getPrimaryInstrument()
        );
    }

    private void validateDifferentUsers(Long requesterId, Long targetUserId) {
        if (requesterId != null && requesterId.equals(targetUserId)) {
            throw new UserApplicationException(
                    UserErrorCode.CANNOT_FOLLOW_SELF,
                    "본인을 팔로우할 수 없습니다."
            );
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
