package glitched.adlips.application.project.dto.response;

import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;
import java.util.List;

public record TrackGetListResponse(ViewerPermission viewerPermission, ProjectSummary project,
                                   List<TrackSummary> tracks) {
    public record ViewerPermission(Long userId, ProjectMemberRole role, boolean canEdit,
                                   boolean canContribute, boolean canReviewContribution, boolean canExport) {}
    public record ProjectSummary(Long projectId, String title, String description, Long albumImageFileId,
                                 String albumImageUrl, Long ownerId, int bpm, String songKey,
                                 int timeSignatureNumerator, int timeSignatureDenominator, int ppq,
                                 int maxDurationMs, long maxTick, ProjectVersionResponse version,
                                 ProjectStatus status, boolean isPublic) {}
    public record TrackSummary(Long trackId, Long ownerId, String name, String instrument,
                               Long mediaFileId, String mediaUrl, int volume, int pan, int sortOrder,
                               boolean isMuted, boolean isSolo, ApprovalStatus approvalStatus,
                               List<ClipSummary> clips) {}
    public record ClipSummary(Long clipId, Long ownerId, ClipType clipType, ClipSourceType sourceType,
                              Long mediaFileId, String mediaUrl, String waveformUrl, int startTick,
                              Integer durationTick, int startTimeMs, Integer durationMs,
                              int clipOffsetMs, ApprovalStatus approvalStatus) {}
}
