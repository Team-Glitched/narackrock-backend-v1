package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.EditorDraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EditorDraftRepositoryPort {
    Optional<EditorDraft> findById(UUID id);
    List<EditorDraft> findActiveByProjectAndOwner(long projectId, long ownerId);
    EditorDraft save(EditorDraft draft);
}
