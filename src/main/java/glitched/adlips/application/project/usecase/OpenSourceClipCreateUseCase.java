package glitched.adlips.application.project.usecase;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.opensource.OpenSourceMediaPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.OpenSourceClipCreateRequest;
import glitched.adlips.application.project.dto.request.OpenSourceTrackCreateRequest;
import glitched.adlips.application.project.dto.response.AudioClipCreateResponse;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.common.TextSanitizer;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.InstrumentFamily;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.project.ProjectTrack;
import java.util.Objects;

/** Places a public open source's audio on the caller's own layer; the media stays owned by the source owner. */
public class OpenSourceClipCreateUseCase {
    private final NotificationEvents notifications;
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final UserRepositoryPort users;
    private final MediaFileRepositoryPort mediaFiles;
    private final OpenSourceMediaPort openSources;
    private final TransactionRunner transactionRunner;

    public OpenSourceClipCreateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                       ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                       UserRepositoryPort users, MediaFileRepositoryPort mediaFiles,
                                       OpenSourceMediaPort openSources, TransactionRunner transactionRunner) {
        this(projects, members, tracks, clips, users, mediaFiles, openSources, transactionRunner, NotificationEvents.none());
    }

    public OpenSourceClipCreateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                       ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                       UserRepositoryPort users, MediaFileRepositoryPort mediaFiles,
                                       OpenSourceMediaPort openSources, TransactionRunner transactionRunner, NotificationEvents notifications) {
        this.projects = projects;
        this.members = members;
        this.tracks = tracks;
        this.clips = clips;
        this.users = users;
        this.mediaFiles = mediaFiles;
        this.openSources = openSources;
        this.transactionRunner = transactionRunner;
    
        this.notifications = notifications;
    }

    public AudioClipCreateResponse execute(OpenSourceClipCreateRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    /** Creates a dedicated bottom sample track containing the public open source's audio clip. */
    public AudioClipCreateResponse createTrack(OpenSourceTrackCreateRequest request) {
        return transactionRunner.required(() -> createTrackInternal(request));
    }

    private AudioClipCreateResponse createTrackInternal(OpenSourceTrackCreateRequest request) {
        if (request == null || request.projectId() == null || request.userId() == null
                || request.openSourceId() == null || request.openSourceId() <= 0) {
            throw error(ProjectErrorCode.VALIDATION_ERROR, "샘플 ID가 올바르지 않습니다.");
        }
        var project = projects.findByIdAndDeletedAtIsNullForUpdate(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "완료된 곡에는 샘플을 추가할 수 없습니다.");
        }
        var user = users.findById(request.userId()).filter(it -> it.isActive())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        validateRange(request.startTick(), request.durationTick(), project.getMaxTick());
        var source = openSources.findAvailable(request.openSourceId())
                .orElseThrow(() -> error(ProjectErrorCode.OPEN_SOURCE_NOT_FOUND, "존재하지 않거나 접근할 수 없는 샘플입니다."));
        var media = mediaFiles.findById(source.mediaFileId())
                .filter(file -> file.getOwnerId().equals(source.ownerId())
                        && file.getFileType() == MediaFileType.AUDIO
                        && file.getStatus() == MediaFileStatus.READY)
                .orElseThrow(() -> error(ProjectErrorCode.MEDIA_FILE_NOT_READY, "음원이 준비되지 않았습니다."));
        var existingTracks = tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(request.projectId());
        boolean alreadyAdded = existingTracks.stream().anyMatch(existing ->
                Objects.equals(existing.getOriginOpenSourceId(), request.openSourceId())
                        && existing.getApprovalStatus() != ApprovalStatus.REJECTED);
        if (alreadyAdded) {
            throw error(ProjectErrorCode.TRACK_LIMIT_EXCEEDED, "이미 이 곡에 추가된 샘플입니다.");
        }
        int nextSortOrder = existingTracks.stream()
                .mapToInt(ProjectTrack::getSortOrder)
                .max()
                .orElse(-1) + 1;
        String trackName = sampleTrackName(source.title());
        String normalizedInstrument = InstrumentFamily.normalize(source.instrument());
        String instrument = InstrumentFamily.of(normalizedInstrument) != null ? normalizedInstrument : "other";
        ProjectTrack track = tracks.save(new ProjectTrack(
                project, user, trackName, instrument, nextSortOrder, request.openSourceId()));
        long durationMs = project.tickToMilliseconds(request.durationTick());
        ProjectClip clip = clips.save(ProjectClip.createAudio(
                project, track, user, ClipSourceType.IMPORTED, media.getId(), null,
                request.startTick(), request.durationTick(), 0, durationMs, 0));
        clip.attributeOpenSource(request.openSourceId());
        notifications.record(SOURCE_IMPORTED, request.userId(), clip.getId(), project.getId(), null);
        return new AudioClipCreateResponse(
                request.projectId(), track.getId(), clip.getId(), clip.getClipType(), clip.getSourceType(),
                clip.getMediaFileId(), media.getFileUrl(), null, clip.getStartTick(), clip.getDurationTick(),
                clip.getStartTimeMs(), clip.getDurationMs(), clip.getClipOffsetMs(), clip.getApprovalStatus(),
                track.getMediaFileId());
    }

    private String sampleTrackName(String title) {
        String sanitized = TextSanitizer.sanitizeSingleLine(title);
        if (sanitized.isEmpty()) {
            return "샘플";
        }
        if (ContentLimits.exceeds(sanitized, ContentLimits.TRACK_NAME_MAX_LENGTH)) {
            int end = sanitized.offsetByCodePoints(0, ContentLimits.TRACK_NAME_MAX_LENGTH);
            return sanitized.substring(0, end);
        }
        return sanitized;
    }

    private AudioClipCreateResponse executeInternal(OpenSourceClipCreateRequest request) {
        if (request == null || request.projectId() == null || request.trackId() == null
                || request.userId() == null || request.openSourceId() == null || request.openSourceId() <= 0) {
            throw error(ProjectErrorCode.VALIDATION_ERROR, "샘플 ID가 올바르지 않습니다.");
        }
        var project = projects.findByIdAndDeletedAtIsNullForUpdate(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "완료된 곡에는 샘플을 추가할 수 없습니다.");
        }
        var track = tracks.findTrackByIdAndIsDeletedFalse(request.trackId())
                .orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND, "존재하지 않는 트랙입니다."));
        if (!track.belongsTo(project)) {
            throw error(ProjectErrorCode.TRACK_NOT_IN_PROJECT, "해당 프로젝트에 속한 트랙이 아닙니다.");
        }
        if (!request.userId().equals(track.getOwner().getId())) {
            throw error(ProjectErrorCode.TRACK_EDIT_DENIED, "본인의 레이어에만 샘플을 추가할 수 있습니다.");
        }
        if (track.isSampleTrack()) {
            throw error(ProjectErrorCode.TRACK_EDIT_DENIED, "샘플 전용 트랙에는 클립을 추가할 수 없습니다.");
        }
        if (InstrumentFamily.of(track.getInstrument()) == null) {
            throw error(ProjectErrorCode.TRACK_INSTRUMENT_NOT_SET, "오디오를 저장하기 전에 악기나 보컬 역할을 선택해 주세요.");
        }
        validateRange(request.startTick(), request.durationTick(), project.getMaxTick());
        var source = openSources.findAvailable(request.openSourceId())
                .orElseThrow(() -> error(ProjectErrorCode.OPEN_SOURCE_NOT_FOUND, "존재하지 않거나 접근할 수 없는 샘플입니다."));
        var media = mediaFiles.findById(source.mediaFileId())
                .filter(file -> file.getOwnerId().equals(source.ownerId())
                        && file.getFileType() == MediaFileType.AUDIO
                        && file.getStatus() == MediaFileStatus.READY)
                .orElseThrow(() -> error(ProjectErrorCode.MEDIA_FILE_NOT_READY, "음원이 준비되지 않았습니다."));
        var user = users.findById(request.userId()).filter(it -> it.isActive())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        long durationMs = project.tickToMilliseconds(request.durationTick());
        int sortOrder = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(request.trackId()).size();
        ProjectClip clip = clips.save(ProjectClip.createAudio(
                project, track, user, ClipSourceType.IMPORTED, media.getId(), null,
                request.startTick(), request.durationTick(), 0, durationMs, sortOrder));
        clip.attributeOpenSource(request.openSourceId());
        notifications.record(SOURCE_IMPORTED,request.userId(),clip.getId(),project.getId(),null);
        return new AudioClipCreateResponse(
                request.projectId(), request.trackId(), clip.getId(), clip.getClipType(), clip.getSourceType(),
                clip.getMediaFileId(), media.getFileUrl(), null, clip.getStartTick(), clip.getDurationTick(),
                clip.getStartTimeMs(), clip.getDurationMs(), clip.getClipOffsetMs(), clip.getApprovalStatus(),
                track.getMediaFileId());
    }

    private void validateRange(int startTick, long durationTick, long maxTick) {
        if (startTick < 0 || durationTick <= 0) {
            throw error(ProjectErrorCode.INVALID_CLIP_RANGE, "클립의 시작 위치 또는 길이가 올바르지 않습니다.");
        }
        if (durationTick > maxTick - startTick) {
            throw error(ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED, "곡은 최대 1분을 초과할 수 없습니다.");
        }
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
