package glitched.adlips.adapter.out.persistence.media;

import glitched.adlips.application.media.port.out.MediaUploadSessionRepositoryPort;
import glitched.adlips.domain.media.MediaUploadSession;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MediaUploadSessionPersistenceAdapter implements MediaUploadSessionRepositoryPort {

    private final MediaUploadSessionJpaRepository sessions;

    public MediaUploadSessionPersistenceAdapter(MediaUploadSessionJpaRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public Optional<MediaUploadSession> findFirstByMediaFileIdOrderByCreatedAtDesc(Long mediaFileId) {
        return sessions.findFirstByMediaFileIdOrderByCreatedAtDesc(mediaFileId);
    }

    @Override
    public MediaUploadSession save(MediaUploadSession session) {
        return sessions.save(session);
    }
}
