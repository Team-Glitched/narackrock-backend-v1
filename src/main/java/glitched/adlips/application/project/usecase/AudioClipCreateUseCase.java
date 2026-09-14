package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.AudioClipCreateRequest;
import glitched.adlips.application.project.dto.response.AudioClipCreateResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMemberRole;
public class AudioClipCreateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final UserRepositoryPort users;
    private final MediaFileRepositoryPort mediaFiles;
    private final TransactionRunner transactionRunner;

    public AudioClipCreateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                  ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                  UserRepositoryPort users, MediaFileRepositoryPort mediaFiles) {
        this(projects, members, tracks, clips, users, mediaFiles, TransactionRunner.direct());
    }

    public AudioClipCreateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                  ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                  UserRepositoryPort users, MediaFileRepositoryPort mediaFiles,
                                  TransactionRunner transactionRunner) {
        this.projects = projects;
        this.members = members;
        this.tracks = tracks;
        this.clips = clips;
        this.users = users;
        this.mediaFiles = mediaFiles;
        this.transactionRunner = transactionRunner;
    }

    public AudioClipCreateResponse execute(AudioClipCreateRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private AudioClipCreateResponse executeInternal(AudioClipCreateRequest request) {
        validateBasic(request);
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        var track = tracks.findTrackByIdAndIsDeletedFalse(request.trackId())
                .orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND, "존재하지 않는 트랙입니다."));
        if (!track.belongsTo(project)) {
            throw error(ProjectErrorCode.TRACK_NOT_IN_PROJECT, "해당 프로젝트에 속한 트랙이 아닙니다.");
        }
        validateRange(request.startTick(), request.durationTick(), project.getMaxTick());
        var media = mediaFiles.findById(request.mediaFileId())
                .orElseThrow(() -> error(ProjectErrorCode.MEDIA_FILE_NOT_FOUND, "존재하지 않는 미디어 파일입니다."));
        if (!media.getOwnerId().equals(request.userId())) {
            throw error(ProjectErrorCode.MEDIA_FILE_ACCESS_DENIED, "해당 미디어 파일을 사용할 권한이 없습니다.");
        }
        if (media.getStatus() != MediaFileStatus.READY) {
            throw error(ProjectErrorCode.MEDIA_FILE_NOT_READY, "아직 업로드가 완료되지 않은 파일입니다.");
        }
        if (media.getFileType() != MediaFileType.AUDIO) {
            throw error(ProjectErrorCode.INVALID_AUDIO_SOURCE_TYPE, "오디오 파일만 클립으로 생성할 수 있습니다.");
        }
        var user = users.findById(request.userId()).filter(it -> it.isActive())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "해당 프로젝트에 접근할 권한이 없습니다."));
        int durationMs = project.tickToMilliseconds(request.durationTick());
        int sortOrder = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(request.trackId()).size();
        ProjectClip clip = clips.save(ProjectClip.createAudio(
                project, track, user, request.sourceType(), request.mediaFileId(), null,
                request.startTick(), request.durationTick(), request.clipOffsetMs(), durationMs, sortOrder));
        return new AudioClipCreateResponse(
                request.projectId(), request.trackId(), clip.getId(), clip.getClipType(), clip.getSourceType(),
                clip.getMediaFileId(), media.getFileUrl(), null, clip.getStartTick(), clip.getDurationTick(),
                clip.getStartTimeMs(), clip.getDurationMs(), clip.getClipOffsetMs(), clip.getApprovalStatus(),
                track.getMediaFileId());
    }

    private void validateBasic(AudioClipCreateRequest request) {
        if (request == null || request.projectId() == null || request.trackId() == null
                || request.userId() == null || request.mediaFileId() == null) {
            throw error(ProjectErrorCode.INVALID_CLIP_RANGE, "클립의 시작 위치 또는 길이가 올바르지 않습니다.");
        }
        if (request.sourceType() != ClipSourceType.RECORDING
                && request.sourceType() != ClipSourceType.FILE_UPLOAD) {
            throw error(ProjectErrorCode.INVALID_AUDIO_SOURCE_TYPE, "올바르지 않은 오디오 소스 타입입니다.");
        }
    }

    private void validateRange(int startTick, int durationTick, long maxTick) {
        if (startTick < 0 || durationTick <= 0) {
            throw error(ProjectErrorCode.INVALID_CLIP_RANGE, "클립의 시작 위치 또는 길이가 올바르지 않습니다.");
        }
        if ((long) startTick + durationTick > maxTick) {
            throw error(ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED, "곡은 최대 1분을 초과할 수 없습니다.");
        }
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
