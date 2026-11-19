package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.TrackGetListRequest;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.dto.response.TrackGetListResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
public class TrackGetListUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final MediaFileRepositoryPort mediaFiles;
    private final ProfileRepositoryPort profiles;
    private final ProjectContributionRepositoryPort contributions;
    private final TransactionRunner transactionRunner;

    public TrackGetListUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                               ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                               MediaFileRepositoryPort mediaFiles, ProfileRepositoryPort profiles,
                               ProjectContributionRepositoryPort contributions) {
        this(projects, members, tracks, clips, mediaFiles, profiles, contributions, TransactionRunner.direct());
    }

    public TrackGetListUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                               ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                               MediaFileRepositoryPort mediaFiles, ProfileRepositoryPort profiles,
                               ProjectContributionRepositoryPort contributions, TransactionRunner transactionRunner) {
        this.projects = projects; this.members = members; this.tracks = tracks;
        this.clips = clips; this.mediaFiles = mediaFiles; this.profiles = profiles;
        this.contributions = contributions;
        this.transactionRunner = transactionRunner;
    }

    public TrackGetListResponse execute(TrackGetListRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private TrackGetListResponse executeInternal(TrackGetListRequest request) {
        if (request == null || request.projectId() == null || request.userId() == null || request.view() == null
                || (request.contributionId() != null && request.contributionId() < 1)) {
            throw error(ProjectErrorCode.VALIDATION_ERROR, "조회 조건이 올바르지 않습니다.");
        }
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않거나 삭제된 음악 프로젝트입니다."));
        var member = members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트를 조회할 권한이 없습니다."));
        ProjectMemberRole role = member.getRole();
        ProjectContribution selected = null;
        if (request.contributionId() != null) {
            selected = contributions.findByIdAndProjectId(request.contributionId(), request.projectId())
                    .orElseThrow(() -> error(ProjectErrorCode.CONTRIBUTION_NOT_FOUND, "해당 프로젝트의 참여 요청을 찾을 수 없습니다."));
            if (role != ProjectMemberRole.OWNER && !selected.getUser().getId().equals(request.userId())) {
                throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 참여 요청을 확인할 권한이 없습니다.");
            }
        }
        Set<Long> selectedTracks = targetIds(selected, ContributionTargetType.TRACK);
        Set<Long> selectedClips = targetIds(selected, ContributionTargetType.CLIP);
        boolean working = selected == null && (request.view() == TrackGetListRequest.View.WORKING
                || (request.view() == TrackGetListRequest.View.STUDIO && !project.isPublic()));
        boolean editable = working && (role == ProjectMemberRole.OWNER || role == ProjectMemberRole.EDITOR);
        var permission = new TrackGetListResponse.ViewerPermission(request.userId(), role, editable,
                editable, role == ProjectMemberRole.OWNER, editable && role == ProjectMemberRole.OWNER);
        var projectSummary = new TrackGetListResponse.ProjectSummary(project.getId(), project.getTitle(),
                project.getDescription(), project.getAlbumImageFileId(), url(project.getAlbumImageFileId()),
                project.getOwner().getId(), project.getBpm(), project.getSongKey(),
                project.getTimeSignatureNumerator(), project.getTimeSignatureDenominator(), project.getPpq(),
                project.getMaxDurationMs(), project.getMaxTick(),
                new ProjectVersionResponse(project.getMajorVersion(), project.getMinorVersion(), project.getDisplayVersion()),
                project.getStatus(), project.isPublic());
        var ownerImages = new HashMap<Long, Optional<String>>();
        var summaries = new ArrayList<TrackGetListResponse.TrackSummary>();
        for (var track : tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(request.projectId())) {
            Long ownerId = track.getOwner().getId();
            boolean approvedTrack = track.getApprovalStatus() == ApprovalStatus.APPROVED;
            boolean ownWorking = working && editable && ownerId.equals(request.userId())
                    && track.getApprovalStatus() != ApprovalStatus.REJECTED;
            var trackClips = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(track.getId()).stream()
                    .filter(clip -> (approvedTrack && clip.getApprovalStatus() == ApprovalStatus.APPROVED)
                            || (ownWorking && clip.getOwner().getId().equals(request.userId())
                                && clip.getApprovalStatus() != ApprovalStatus.REJECTED)
                            || selectedClips.contains(clip.getId()))
                    .toList();
            boolean requestedTrack = selectedTracks.contains(track.getId())
                    || trackClips.stream().anyMatch(clip -> selectedClips.contains(clip.getId()));
            if (!approvedTrack && !ownWorking && !requestedTrack) continue;
            if (trackClips.isEmpty() && !ownWorking && !requestedTrack) continue;

            var clipSummaries = trackClips.stream()
                    .map(clip -> new TrackGetListResponse.ClipSummary(clip.getId(), clip.getOwner().getId(),
                            clip.getClipType(), clip.getSourceType(), clip.getMediaFileId(), url(clip.getMediaFileId()),
                            url(clip.getWaveformFileId()), clip.getStartTick(), clip.getDurationTick(),
                            clip.getStartTimeMs(), clip.getDurationMs(), clip.getClipOffsetMs(),
                            clip.getApprovalStatus(), clip.getMidiNotes(), clip.getAudioAnalysis()))
                    .toList();
            var ownerImage = ownerImages.computeIfAbsent(ownerId, id ->
                    profiles.findByUserId(id).map(Profile::getProfileImageFileId).map(this::url));
            summaries.add(new TrackGetListResponse.TrackSummary(track.getId(), ownerId,
                    track.getName(), track.getInstrument(), track.getMediaFileId(), url(track.getMediaFileId()),
                    track.getVolume(), track.getPan(), track.getSortOrder(), track.isMuted(), track.isSolo(),
                    track.getApprovalStatus(), clipSummaries, ownerImage.orElse(null)));
        }
        return new TrackGetListResponse(permission, projectSummary, List.copyOf(summaries));
    }

    private Set<Long> targetIds(ProjectContribution contribution, ContributionTargetType type) {
        if (contribution == null) return Set.of();
        return contribution.getItems().stream()
                .filter(item -> item.getTargetType() == type && item.getChangeType() != ContributionChangeType.DELETE)
                .map(item -> item.getTargetId()).collect(Collectors.toSet());
    }

    private String url(Long id) {
        return id == null || id <= 0 ? null : mediaFiles.findById(id).map(it -> it.getFileUrl()).orElse(null);
    }
    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
