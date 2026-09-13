package glitched.adlips.adapter.out.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glitched.adlips.application.user.profile.dto.request.UserProfileImageUpdateRequest;
import glitched.adlips.application.user.profile.port.out.FileStoragePort.StoredFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageAdapterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void savesImageUnderConfiguredLocalDirectory() throws Exception {
        LocalFileStorageAdapter adapter = new LocalFileStorageAdapter(
                tempDirectory.toString(),
                "http://localhost:8080/files"
        );
        byte[] content = new byte[]{4, 5, 6};

        StoredFile stored = adapter.store(
                new UserProfileImageUpdateRequest(1L, "avatar.png", "image/png", content)
        );

        Path savedPath = tempDirectory.resolve(stored.storageKey());
        assertTrue(Files.exists(savedPath));
        assertArrayEquals(content, Files.readAllBytes(savedPath));
        assertTrue(stored.storageKey().startsWith("profiles/1/"));
        assertTrue(stored.url().startsWith("http://localhost:8080/files/profiles/1/"));
    }
}
