package glitched.adlips.application.user.relation;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowerGetListUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileQueryPort profileQuery;
    private final FollowRepositoryPort followRepository;
    private final MediaFileRepositoryPort mediaFileRepository;

    public FollowerGetListUseCase(
            UserRepositoryPort userRepository,
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository
    ) {
        this.userRepository = userRepository;
        this.profileQuery = profileQuery;
        this.followRepository = followRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    public List<UserCard> execute(Long userId) {
        requireActiveUser(userId);
        return profileQuery.findAllByUserIds(followRepository.findFollowerIds(userId)).stream()
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
                profile.getUserId(), profile.getNickname(), profileImageUrl, profile.getPrimaryInstrument()
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
}
