package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.TrackGetListRequest;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.dto.response.TrackGetListResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.project.ProjectMemberRole;
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
    private final TransactionRunner transactionRunner;

    public TrackGetListUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                               ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                               MediaFileRepositoryPort mediaFiles, ProfileRepositoryPort profiles) {
        this(projects, members, tracks, clips, mediaFiles, profiles, TransactionRunner.direct());
    }

    public TrackGetListUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                               ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                               MediaFileRepositoryPort mediaFiles, ProfileRepositoryPort profiles, TransactionRunner transactionRunner) {
        this.projects = projects; this.members = members; this.tracks = tracks;
        this.clips = clips; this.mediaFiles = mediaFiles; this.profiles = profiles;
        this.transactionRunner = transactionRunner;
    }

    public TrackGetListResponse execute(TrackGetListRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private TrackGetListResponse executeInternal(TrackGetListRequest request) {
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않거나 삭제된 음악 프로젝트입니다."));
        var member = members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트를 조회할 권한이 없습니다."));
        ProjectMemberRole role = member.getRole();
        boolean editable = role == ProjectMemberRole.OWNER || role == ProjectMemberRole.EDITOR;
        var permission = new TrackGetListResponse.ViewerPermission(request.userId(), role, editable,
                editable, role == ProjectMemberRole.OWNER, role == ProjectMemberRole.OWNER);
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
            var trackClips = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(track.getId());
            if (trackClips.isEmpty() && !ownerId.equals(request.userId())) continue;

            var clipSummaries = trackClips.stream()
                    .map(clip -> new TrackGetListResponse.ClipSummary(clip.getId(), clip.getOwner().getId(),
                            clip.getClipType(), clip.getSourceType(), clip.getMediaFileId(), url(clip.getMediaFileId()),
                            url(clip.getWaveformFileId()), clip.getStartTick(), clip.getDurationTick(),
                            clip.getStartTimeMs(), clip.getDurationMs(), clip.getClipOffsetMs(),
                            clip.getApprovalStatus(), clip.getMidiNotes()))
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

    private String url(Long id) {
        return id == null || id <= 0 ? null : mediaFiles.findById(id).map(it -> it.getFileUrl()).orElse(null);
    }
    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
