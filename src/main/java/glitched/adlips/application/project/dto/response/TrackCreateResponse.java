package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ApprovalStatus;

public record TrackCreateResponse(Long trackId, Long projectId, String name, String instrument,
                                  Long mediaFileId, String mediaUrl, int volume, int pan,
                                  int sortOrder, boolean isMuted, boolean isSolo,
                                  ApprovalStatus approvalStatus) {
}
