package glitched.adlips.adapter.out.persistence.media;

import glitched.adlips.application.user.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataMediaFileRepository extends JpaRepository<MediaFileJpaEntity, Long> {
}

@Repository
public class MediaFilePersistenceAdapter implements MediaFileRepositoryPort {
    private final SpringDataMediaFileRepository repository;

    public MediaFilePersistenceAdapter(SpringDataMediaFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MediaFile> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public MediaFile save(MediaFile file) {
        MediaFileJpaEntity saved = repository.save(new MediaFileJpaEntity(
                file.getId(),
                file.getOwnerId(),
                file.getFileUrl(),
                file.getStorageKey(),
                file.getOriginalFilename(),
                file.getFileType(),
                file.getMimeType(),
                file.getFileSize(),
                file.getStatus()
        ));
        return toDomain(saved);
    }

    private MediaFile toDomain(MediaFileJpaEntity entity) {
        return MediaFile.restore(
                entity.getId(),
                entity.getOwnerId(),
                entity.getFileUrl(),
                entity.getStorageKey(),
                entity.getOriginalFilename(),
                entity.getFileType(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getStatus()
        );
    }
}
