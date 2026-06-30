package glitched.adlips.application.user.profile;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.TransactionPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort.StoredFile;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileLinkPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

public final class ProfileService {
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final FollowRepositoryPort followRepository;
    private final FileStoragePort fileStorage;
    private final ProfileLinkPort profileLinkPort;
    private final TransactionPort transactionPort;
    private final Clock clock;

    public ProfileService(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FollowRepositoryPort followRepository,
            FileStoragePort fileStorage,
            ProfileLinkPort profileLinkPort,
            TransactionPort transactionPort,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.followRepository = followRepository;
        this.fileStorage = fileStorage;
        this.profileLinkPort = profileLinkPort;
        this.transactionPort = transactionPort;
        this.clock = clock;
    }

    public ProfileView getProfile(Long targetUserId, Long requesterId) {
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
        return new ProfileView(
                profile.getUserId(),
                profile.getNickname(),
                profileImageUrl,
                profile.getExplanation(),
                profile.getPrimaryInstrument(),
                new ProfileView.Relations(
                        profile.getFollowerCount(), profile.getFollowingCount(), following
                ),
                ProfileView.Activities.empty()
        );
    }

    public ProfileUpdateResult updateProfile(Long userId, UpdateProfileCommand command) {
        if (command == null) {
            throw validation("수정할 프로필 정보를 입력해 주세요.");
        }
        String nickname = normalizeNickname(command.nickname());
        return transactionPort.required(() -> {
            requireActiveUser(userId);
            Profile profile = requireProfile(userId);
            if (nickname != null && profileRepository.existsByNicknameAndUserIdNot(nickname, userId)) {
                throw new UserApplicationException(
                        UserErrorCode.DUPLICATE_NICKNAME,
                        "이미 사용중인 닉네임입니다."
                );
            }
            Profile saved = profileRepository.save(profile.update(
                    nickname,
                    command.primaryInstrument(),
                    command.explanation(),
                    LocalDateTime.now(clock)
            ));
            return new ProfileUpdateResult(
                    saved.getNickname(), saved.getPrimaryInstrument(), saved.getExplanation()
            );
        });
    }

    public ProfileImageResult updateProfileImage(Long userId, ProfileImageCommand image) {
        validateImage(image);
        requireActiveUser(userId);
        requireProfile(userId);

        StoredFile stored = fileStorage.store(image);
        try {
            return transactionPort.required(() -> {
                MediaFile mediaFile = mediaFileRepository.save(MediaFile.readyImage(
                        userId,
                        stored.url(),
                        stored.storageKey(),
                        image.originalFilename(),
                        image.mimeType(),
                        image.content().length
                ));
                Profile profile = requireProfile(userId);
                profileRepository.save(profile.changeProfileImage(mediaFile.getId()));
                return new ProfileImageResult(stored.url());
            });
        } catch (RuntimeException exception) {
            fileStorage.delete(stored.storageKey());
            throw exception;
        }
    }

    public ProfileShareResult shareProfile(Long userId) {
        requireActiveUser(userId);
        requireProfile(userId);
        return new ProfileShareResult(userId, profileLinkPort.create(userId));
    }

    private void validateImage(ProfileImageCommand image) {
        if (image == null || image.content() == null || image.content().length == 0
                || image.mimeType() == null || !SUPPORTED_IMAGE_TYPES.contains(image.mimeType())) {
            throw new UserApplicationException(
                    UserErrorCode.INVALID_IMAGE_FILE,
                    "지원하지 않는 이미지 파일 형식입니다."
            );
        }
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        if (nickname.isBlank()) {
            throw validation("닉네임이 입력되지 않았습니다.");
        }
        return nickname.trim();
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

    private UserApplicationException validation(String message) {
        return new UserApplicationException(UserErrorCode.VALIDATION_ERROR, message);
    }
}
