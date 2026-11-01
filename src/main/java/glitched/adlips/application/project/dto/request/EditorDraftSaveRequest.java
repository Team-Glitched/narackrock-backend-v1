package glitched.adlips.application.project.dto.request;

import glitched.adlips.domain.project.EditorDraftSnapshot;
import java.util.UUID;

public record EditorDraftSaveRequest(long expectedRevision, UUID mutationId, Long targetClipId,
                                     EditorDraftSnapshot content, EditorDraftSnapshot baseClip) {}
