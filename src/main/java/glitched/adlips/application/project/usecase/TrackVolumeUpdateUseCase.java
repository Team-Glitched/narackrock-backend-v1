package glitched.adlips.application.project.usecase;

import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.TrackVolumeUpdateRequest;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.dto.response.TrackVolumeUpdateResponse;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;

public class TrackVolumeUpdateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectTrackRepositoryPort tracks;
    private final TransactionRunner transactionRunner;

    public TrackVolumeUpdateUseCase(ProjectRepositoryPort projects, ProjectTrackRepositoryPort tracks) {
        this(projects, tracks, TransactionRunner.direct());
    }

    public TrackVolumeUpdateUseCase(ProjectRepositoryPort projects, ProjectTrackRepositoryPort tracks,
                                    TransactionRunner transactionRunner) {
        this.projects = projects;
        this.tracks = tracks;
        this.transactionRunner = transactionRunner;
    }

    public TrackVolumeUpdateResponse execute(TrackVolumeUpdateRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private TrackVolumeUpdateResponse executeInternal(TrackVolumeUpdateRequest request) {
        if (request == null || request.volume() < 0 || request.volume() > 100) {
            throw error(ProjectErrorCode.INVALID_TRACK_VOLUME, "트랙 볼륨은 0부터 100 사이여야 합니다.");
        }
        // Take the project lock used by sample insertion, review, and deletion before loading the track,
        // so a stale track row cannot be written back over a concurrent deletion.
        Long projectId = tracks.findProjectIdByTrackIdAndIsDeletedFalse(request.trackId())
                .orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND, "존재하지 않는 트랙입니다."));
        projects.findByIdAndDeletedAtIsNullForUpdate(projectId)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        var track = tracks.findTrackByIdAndIsDeletedFalse(request.trackId())
                .orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND, "존재하지 않는 트랙입니다."));
        Long ownerId = track.getOwner().getId();
        Long projectOwnerId = track.getProject().getOwner().getId();
        if (!request.userId().equals(ownerId) && !request.userId().equals(projectOwnerId)) {
            throw error(ProjectErrorCode.TRACK_EDIT_DENIED, "트랙을 수정할 권한이 없습니다.");
        }

        track.changeVolume(request.volume());
        var project = track.getProject();
        return new TrackVolumeUpdateResponse(request.trackId(), track.getVolume(), track.getMediaFileId(),
                new ProjectVersionResponse(project.getMajorVersion(), project.getMinorVersion(),
                        project.getDisplayVersion()));
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
