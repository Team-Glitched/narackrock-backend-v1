package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.response.TrackGetListResponse.ClipSummary;
import glitched.adlips.application.project.dto.response.TrackGetListResponse.TrackSummary;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.project.ApprovalStatus;
import java.util.List;

public class GetShortsParticipantTracksUseCase {
    private final ShortsCompositionQueryPort shorts;
    private final ProjectRepositoryPort projects;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final MediaFileRepositoryPort media;
    private final TransactionRunner transactions;

    public GetShortsParticipantTracksUseCase(ShortsCompositionQueryPort shorts, ProjectRepositoryPort projects,
            ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
            MediaFileRepositoryPort media, TransactionRunner transactions) {
        this.shorts = shorts; this.projects = projects; this.tracks = tracks;
        this.clips = clips; this.media = media; this.transactions = transactions;
    }

    public ShortsParticipantTracksResult execute(Long shortId) {
        return transactions.readOnly(() -> {
            var shortInfo = shorts.findByShortId(shortId).orElseThrow(this::notFound);
            if (shortInfo.projectId() == null || shortInfo.projectDeletedAt() != null) throw notFound();
            var project = projects.findByIdAndDeletedAtIsNull(shortInfo.projectId()).orElseThrow(this::notFound);
            List<TrackSummary> approvedTracks = tracks
                    .findByProjectIdAndApprovalStatusAndIsDeletedFalse(shortInfo.projectId(), ApprovalStatus.APPROVED)
                    .stream().map(track -> new TrackSummary(track.getId(), track.getOwner().getId(),
                            track.getName(), track.getInstrument(), null, null,
                            track.getVolume(), track.getPan(), track.getSortOrder(), track.isMuted(), track.isSolo(),
                            track.getApprovalStatus(), clips
                            .findByTrackIdAndApprovalStatusAndIsDeletedFalseOrderByStartTickAscIdAsc(
                                    track.getId(), ApprovalStatus.APPROVED)
                            .stream().map(clip -> new ClipSummary(clip.getId(), clip.getOwner().getId(),
                                    clip.getClipType(), clip.getSourceType(), clip.getMediaFileId(), url(clip.getMediaFileId()),
                                    null, clip.getStartTick(), clip.getDurationTick(), clip.getStartTimeMs(),
                                    clip.getDurationMs(), clip.getClipOffsetMs(), clip.getApprovalStatus(), clip.getMidiNotes()))
                            .toList(), null)).toList();
            return new ShortsParticipantTracksResult(shortId,
                    new ShortsParticipantTracksResult.ProjectTiming(project.getBpm(), project.getPpq(), project.getMaxDurationMs()),
                    approvedTracks);
        });
    }

    private String url(Long id) {
        return id == null ? null : media.findById(id).filter(file -> file.getStatus() == MediaFileStatus.READY)
                .map(file -> file.getFileUrl()).orElse(null);
    }

    private ShortsComposersApplicationException notFound() {
        return new ShortsComposersApplicationException(ShortsComposersErrorCode.SHORTS_NOT_FOUND,
                "참여자 트랙을 조회할 수 있는 곡을 찾을 수 없습니다.");
    }
}
