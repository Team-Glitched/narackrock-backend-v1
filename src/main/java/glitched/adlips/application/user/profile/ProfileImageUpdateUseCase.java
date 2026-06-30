package glitched.adlips.application.user.profile;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort.StoredFile;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileImageUpdateUseCase {
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final FileStoragePort fileStorage;

    public ProfileImageUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FileStoragePort fileStorage
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public ProfileImageResult execute(Long userId, ProfileImageCommand image) {
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
            return new ProfileImageResult(stored.url());
        } catch (RuntimeException exception) {
            fileStorage.delete(stored.storageKey());
            throw exception;
        }
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
