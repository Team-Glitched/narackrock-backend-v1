package glitched.adlips.application.media.port.out;

import glitched.adlips.domain.media.MediaUploadSession;
import java.util.Optional;

public interface MediaUploadSessionRepositoryPort {

    Optional<MediaUploadSession> findFirstByMediaFileIdOrderByCreatedAtDesc(Long mediaFileId);

    MediaUploadSession save(MediaUploadSession session);
}
