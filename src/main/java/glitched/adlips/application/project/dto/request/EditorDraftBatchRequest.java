package glitched.adlips.application.project.dto.request;

import java.util.List;
import java.util.UUID;

public record EditorDraftBatchRequest(List<Reference> drafts, String description, ProjectVersionRequest baseProjectVersion) {
    public record Reference(UUID draftId, long expectedRevision) {}
}
