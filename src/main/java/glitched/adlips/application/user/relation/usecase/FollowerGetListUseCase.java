package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.user.relation.dto.request.FollowerGetListRequest;
import glitched.adlips.application.user.relation.dto.response.UserRelationResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.List;
public class FollowerGetListUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileQueryPort profileQuery;
    private final FollowRepositoryPort followRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final TransactionRunner transactionRunner;

    public FollowerGetListUseCase(
            UserRepositoryPort userRepository,
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository
    ) {
        this(userRepository, profileQuery, followRepository, mediaFileRepository, TransactionRunner.direct());
    }

    public FollowerGetListUseCase(
            UserRepositoryPort userRepository,
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.profileQuery = profileQuery;
        this.followRepository = followRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.transactionRunner = transactionRunner;
    }

    public List<UserRelationResponse> execute(FollowerGetListRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private List<UserRelationResponse> executeInternal(FollowerGetListRequest request) {
        Long userId = request.userId();
        requireActiveUser(userId);
        return profileQuery.findAllByUserIds(followRepository.findFollowerIds(userId)).stream()
                .map(this::toUserCard)
                .toList();
    }

    private UserRelationResponse toUserCard(Profile profile) {
        String profileImageUrl = profile.getProfileImageFileId() == null
                ? null
                : mediaFileRepository.findById(profile.getProfileImageFileId())
                        .map(MediaFile::getFileUrl)
                        .orElse(null);
        return new UserRelationResponse(
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
