package glitched.adlips.adapter.out.storage;

import glitched.adlips.application.user.profile.dto.request.UserProfileImageUpdateRequest;
import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageAdapter implements FileStoragePort, MediaContentStoragePort {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path rootDirectory;
    private final String publicBaseUrl;

    public LocalFileStorageAdapter(
            @Value("${app.storage.local-root}") String rootDirectory,
            @Value("${app.storage.public-base-url}") String publicBaseUrl
    ) {
        this.rootDirectory = Path.of(rootDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    @Override
    public StoredFile store(UserProfileImageUpdateRequest image) {
        String extension = EXTENSIONS.get(image.mimeType());
        if (extension == null) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
        String storageKey = "profiles/" + UUID.randomUUID() + extension;
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(
                    target,
                    image.content(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
            return new StoredFile(storageKey, publicBaseUrl + "/" + storageKey);
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 파일 저장에 실패했습니다.", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 파일 삭제에 실패했습니다.", exception);
        }
    }

    public void put(String storageKey, byte[] content, String contentType) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(metadataPath(target), contentType,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 파일 저장에 실패했습니다.", exception);
        }
    }

    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolve(storageKey));
    }

    public long size(String storageKey) {
        try {
            return Files.size(resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 파일 크기 확인에 실패했습니다.", exception);
        }
    }

    public String contentType(String storageKey) {
        try {
            return Files.readString(metadataPath(resolve(storageKey)));
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 파일 형식 확인에 실패했습니다.", exception);
        }
    }

    public String publicUrl(String storageKey) {
        return publicBaseUrl + "/" + storageKey;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    private Path resolve(String storageKey) {
        Path target = rootDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("유효하지 않은 파일 경로입니다.");
        }
        return target;
    }

    private Path metadataPath(Path target) {
        return target.resolveSibling(target.getFileName() + ".content-type");
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
