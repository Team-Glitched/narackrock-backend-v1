package glitched.adlips.application.user.port.out;

import glitched.adlips.application.user.ProfileImageCommand;

public interface FileStoragePort {
    StoredFile store(ProfileImageCommand image);

    default void delete(String storageKey) {
    }

    record StoredFile(String storageKey, String url) {
    }
}
