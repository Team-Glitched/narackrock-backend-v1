package glitched.adlips.adapter.out.persistence.media;

import glitched.adlips.domain.media.MediaUploadSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaUploadSessionJpaRepository extends JpaRepository<MediaUploadSession, Long> {
    Optional<MediaUploadSession> findFirstByMediaFileIdOrderByCreatedAtDesc(Long mediaFileId);
}
