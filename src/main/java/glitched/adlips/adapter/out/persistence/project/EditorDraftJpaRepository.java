package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.domain.project.EditorDraft;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditorDraftJpaRepository extends JpaRepository<EditorDraftEntity, UUID> {
    List<EditorDraftEntity> findByProjectIdAndStatus(long projectId, EditorDraft.Status status);
    List<EditorDraftEntity> findByProjectIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(
            long projectId, long ownerId, EditorDraft.Status status);
}
