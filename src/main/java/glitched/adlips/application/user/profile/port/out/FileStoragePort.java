package glitched.adlips.application.user.profile.port.out;

import glitched.adlips.application.user.profile.ProfileImageCommand;

public interface FileStoragePort {
    StoredFile store(ProfileImageCommand image);

    default void delete(String storageKey) {
    }

    record StoredFile(String storageKey, String url) {
    }
}
