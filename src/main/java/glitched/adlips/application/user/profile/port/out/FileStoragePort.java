package glitched.adlips.application.user.profile.port.out;

import glitched.adlips.application.user.profile.dto.request.UserProfileImageUpdateRequest;

public interface FileStoragePort {
    StoredFile store(UserProfileImageUpdateRequest image);

    default void delete(String storageKey) {
    }

    record StoredFile(String storageKey, String url) {
    }
}
