package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.application.project.port.out.EditorDraftRepositoryPort;
import glitched.adlips.domain.project.EditorDraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EditorDraftPersistenceAdapter implements EditorDraftRepositoryPort {
    private final EditorDraftJpaRepository repository;
    public EditorDraftPersistenceAdapter(EditorDraftJpaRepository repository) { this.repository = repository; }

    @Override public Optional<EditorDraft> findById(UUID id) {
        return repository.findById(id).map(EditorDraftEntity::toDomain);
    }

    @Override public List<EditorDraft> findActiveByProjectAndOwner(long projectId, long ownerId) {
        return repository.findByProjectIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(projectId, ownerId, EditorDraft.Status.ACTIVE)
                .stream().map(EditorDraftEntity::toDomain).toList();
    }

    @Override public List<EditorDraft> findActiveByProject(long projectId) {
        return repository.findByProjectIdAndStatus(projectId, EditorDraft.Status.ACTIVE)
                .stream().map(EditorDraftEntity::toDomain).toList();
    }

    @Override public EditorDraft save(EditorDraft draft) {
        return repository.save(new EditorDraftEntity(draft)).toDomain();
    }
}
