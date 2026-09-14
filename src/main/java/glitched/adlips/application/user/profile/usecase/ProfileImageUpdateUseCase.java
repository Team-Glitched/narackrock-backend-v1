package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.user.profile.dto.request.UserProfileImageUpdateRequest;
import glitched.adlips.application.user.profile.dto.response.UserProfileImageUpdateResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort.StoredFile;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.Set;
public class ProfileImageUpdateUseCase {
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final FileStoragePort fileStorage;
    private final TransactionRunner transactionRunner;

    public ProfileImageUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FileStoragePort fileStorage
    ) {
        this(userRepository, profileRepository, mediaFileRepository, fileStorage, TransactionRunner.direct());
    }

    public ProfileImageUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FileStoragePort fileStorage,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.fileStorage = fileStorage;
        this.transactionRunner = transactionRunner;
    }

    public UserProfileImageUpdateResponse execute(UserProfileImageUpdateRequest image) {
        return transactionRunner.required(() -> executeInternal(image));
    }

    private UserProfileImageUpdateResponse executeInternal(UserProfileImageUpdateRequest image) {
        Long userId = image == null ? null : image.userId();
        validateImage(image);
        requireActiveUser(userId);
        requireProfile(userId);

        StoredFile stored = fileStorage.store(image);
        try {
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
            return new UserProfileImageUpdateResponse(stored.url());
        } catch (RuntimeException exception) {
            fileStorage.delete(stored.storageKey());
            throw exception;
        }
    }

    private void validateImage(UserProfileImageUpdateRequest image) {
        if (image == null || image.content() == null || image.content().length == 0
                || image.mimeType() == null || !SUPPORTED_IMAGE_TYPES.contains(image.mimeType())) {
            throw new UserApplicationException(
                    UserErrorCode.INVALID_IMAGE_FILE,
                    "지원하지 않는 이미지 파일 형식입니다."
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
