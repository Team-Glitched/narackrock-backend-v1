package glitched.adlips.application.project.usecase;

import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.AudioAnalysis;
import glitched.adlips.domain.project.AudioSoundKind;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.MidiNote;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMemberRole;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AnalyzeAudioClipUseCase {
    private static final int MAX_BYTES = 25 * 1024 * 1024;
    private final ProjectClipRepositoryPort clips;
    private final ProjectMemberRepositoryPort members;
    private final MediaFileRepositoryPort media;
    private final MediaContentStoragePort storage;
    private final HtiMidiTransformPort inference;
    private final TransactionRunner transactions;
    private final String recipeVersion;

    public AnalyzeAudioClipUseCase(ProjectClipRepositoryPort clips, ProjectMemberRepositoryPort members,
                                   MediaFileRepositoryPort media, MediaContentStoragePort storage,
                                   HtiMidiTransformPort inference, TransactionRunner transactions,
                                   String recipeVersion) {
        this.clips = clips;
        this.members = members;
        this.media = media;
        this.storage = storage;
        this.inference = inference;
        this.transactions = transactions;
        this.recipeVersion = recipeVersion;
    }

    public AudioAnalysis execute(Long projectId, Long clipId, Long userId, AudioSoundKind kind, boolean refresh) {
        if (kind == null) throw error(ProjectErrorCode.VALIDATION_ERROR, "분석할 소리 종류를 선택해 주세요.");
        var snapshot = transactions.readOnly(() -> snapshot(authorizedClip(projectId, userId, clips.findClipByIdAndIsDeletedFalse(clipId))));
        if (!refresh && reusable(snapshot.cached(), kind)) return snapshot.cached();

        var source = snapshot.source();
        var file = media.findById(source.mediaId()).orElseThrow(() ->
                error(ProjectErrorCode.MEDIA_FILE_NOT_FOUND, "원본 오디오를 찾을 수 없습니다."));
        if (!Objects.equals(file.getOwnerId(), userId)) {
            throw error(ProjectErrorCode.MEDIA_FILE_ACCESS_DENIED, "원본 오디오를 사용할 권한이 없습니다.");
        }
        if (file.getFileType() != MediaFileType.AUDIO || file.getStatus() != MediaFileStatus.READY
                || file.getFileSize() > MAX_BYTES || source.durationMs() > 60_050) {
            throw error(ProjectErrorCode.INVALID_HTI_AUDIO, "60초, 25MB 이하의 오디오 클립만 분석할 수 있습니다.");
        }

        byte[] content;
        try {
            content = storage.get(file.getStorageKey());
        } catch (IllegalStateException exception) {
            throw error(ProjectErrorCode.MEDIA_FILE_NOT_FOUND, "원본 오디오를 읽을 수 없습니다.");
        }

        AudioAnalysis analysis;
        try {
            var result = inference.analyze(new HtiMidiTransformPort.AudioCommand(
                    kind, file.getOriginalFilename(), file.getMimeType(), content, snapshot.bpm(), snapshot.ppq(),
                    source.offsetMs(), source.durationMs()));
            if (result == null || result.engineVersion() == null || result.midiNotes() == null) {
                throw new HtiMidiTransformPort.HtiServiceException("Missing audio analysis result", null);
            }
            analysis = new AudioAnalysis(kind, result.engineVersion(), recipeVersion, snapshot.bpm(), snapshot.ppq(),
                    clipNotes(result.midiNotes(), snapshot.maxNoteTick()));
        } catch (HtiMidiTransformPort.HtiServiceException | IllegalArgumentException exception) {
            throw error(ProjectErrorCode.HTI_TRANSFORM_FAILED,
                    "오디오 분석에 실패했습니다. 원본은 유지되며 다시 시도할 수 있습니다.");
        }

        return transactions.required(() -> {
            var current = authorizedClip(projectId, userId, clips.findClipForUpdate(clipId));
            if (!source.equals(source(current))) {
                throw error(ProjectErrorCode.EDITOR_DRAFT_BASE_CHANGED, "원본 오디오가 바뀌었습니다. 다시 분석해 주세요.");
            }
            if (!refresh && reusable(current.getAudioAnalysis(), kind)) return current.getAudioAnalysis();
            current.storeAudioAnalysis(analysis);
            clips.save(current);
            return analysis;
        });
    }

    private boolean reusable(AudioAnalysis analysis, AudioSoundKind kind) {
        return analysis != null && analysis.soundKind() == kind && recipeVersion.equals(analysis.recipeVersion());
    }

    private ProjectClip authorizedClip(Long projectId, Long userId, Optional<ProjectClip> candidate) {
        var clip = candidate
                .filter(item -> Objects.equals(item.getProject().getId(), projectId)
                        && item.getProject().getDeletedAt() == null && !item.getTrack().isDeleted())
                .orElseThrow(() -> error(ProjectErrorCode.CLIP_NOT_FOUND, "오디오 클립을 찾을 수 없습니다."));
        if (!Objects.equals(clip.getOwner().getId(), userId)
                || members.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER).isEmpty()) {
            throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "내 오디오 클립만 분석할 수 있습니다.");
        }
        if (clip.getClipType() != ClipType.AUDIO || clip.getMediaFileId() == null
                || (clip.getSourceType() != ClipSourceType.RECORDING && clip.getSourceType() != ClipSourceType.FILE_UPLOAD)
                || clip.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw error(ProjectErrorCode.INVALID_HTI_AUDIO, "분석할 수 있는 오디오 클립이 아닙니다.");
        }
        return clip;
    }

    private static List<MidiNote> clipNotes(List<HtiMidiTransformPort.Note> notes, long maxNoteTick) {
        var result = new ArrayList<MidiNote>();
        var keys = new HashSet<NoteKey>();
        for (var note : notes) {
            long end = Math.min(maxNoteTick, (long) note.startTick() + note.durationTick());
            if (note.startTick() < 0 || end <= note.startTick()) continue;
            int duration = Math.toIntExact(end - note.startTick());
            if (keys.add(new NoteKey(note.pitch(), note.startTick(), duration))) {
                result.add(new MidiNote(note.pitch(), note.startTick(), duration, note.velocity(),
                        note.effectType(), note.effectParams()));
            }
        }
        return result;
    }

    private static Source source(ProjectClip clip) {
        return new Source(clip.getMediaFileId(), clip.getClipOffsetMs(), clip.getDurationMs());
    }

    private static Snapshot snapshot(ProjectClip clip) {
        return new Snapshot(source(clip), clip.getProject().getBpm(), clip.getProject().getPpq(),
                clip.getDurationTick(), clip.getAudioAnalysis());
    }

    private record Source(Long mediaId, int offsetMs, long durationMs) {}
    private record Snapshot(Source source, int bpm, int ppq, long maxNoteTick, AudioAnalysis cached) {}
    private record NoteKey(int pitch, int start, int duration) {}

    private static ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
