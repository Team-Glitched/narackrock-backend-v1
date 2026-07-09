package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.user.profile.dto.request.UserProfileGetRequest;
import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileActivityQueryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfileGetUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final FollowRepositoryPort followRepository;
    private final ProfileActivityQueryPort profileActivityQueryPort;

    public ProfileGetUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FollowRepositoryPort followRepository,
            ProfileActivityQueryPort profileActivityQueryPort
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.followRepository = followRepository;
        this.profileActivityQueryPort = profileActivityQueryPort;
    }

    public UserProfileGetResponse execute(UserProfileGetRequest request) {
        Long targetUserId = request.targetUserId();
        Long requesterId = request.requesterId();
        requireActiveUser(targetUserId);
        Profile profile = requireProfile(targetUserId);
        boolean owner = targetUserId.equals(requesterId);
        if (profile.isPrivate() && !owner) {
            throw new UserApplicationException(UserErrorCode.PRIVATE_PROFILE, "비공개 프로필입니다.");
        }

        String profileImageUrl = profile.getProfileImageFileId() == null
                ? null
                : mediaFileRepository.findById(profile.getProfileImageFileId())
                        .map(MediaFile::getFileUrl)
                        .orElse(null);
        boolean following = !owner && requesterId != null && followRepository.exists(requesterId, targetUserId);
        return new UserProfileGetResponse(
                profile.getUserId(),
                profile.getNickname(),
                profileImageUrl,
                profile.getExplanation(),
                profile.getPrimaryInstrument(),
                new UserProfileGetResponse.Relations(
                        profile.getFollowerCount(), profile.getFollowingCount(), following
                ),
                profileActivityQueryPort.findByUserId(targetUserId)
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
