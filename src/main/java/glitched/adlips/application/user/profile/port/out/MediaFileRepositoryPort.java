package glitched.adlips.application.user.profile.port.out;

import glitched.adlips.domain.media.MediaFile;
import java.util.Optional;

public interface MediaFileRepositoryPort {
    Optional<MediaFile> findById(Long id);

    MediaFile save(MediaFile file);
}
