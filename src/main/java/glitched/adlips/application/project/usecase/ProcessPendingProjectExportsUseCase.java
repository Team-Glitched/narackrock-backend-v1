package glitched.adlips.application.project.usecase;

import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectExportProcessingResult;
import glitched.adlips.application.project.port.out.ProjectAudioMixerPort;
import glitched.adlips.application.project.port.out.ProjectAudioMixerPort.AudioLayer;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort.Layer;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.ExportStatus;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectTrack;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessPendingProjectExportsUseCase {

    private static final String MIXED_AUDIO_MIME_TYPE = "audio/wav";
    private static final String ARCHIVE_MIME_TYPE = "application/zip";

    private final ProjectExportRepositoryPort exports;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final MediaFileRepositoryPort mediaFiles;
    private final MediaContentStoragePort storage;
    private final ProjectAudioMixerPort mixer;
    private final ProjectLayerArchivePort archiver;
    private final PublishedShortPort publishedShorts;
    private final Clock clock;
    private final int batchSize;
    private final TransactionRunner transactionRunner;

    public ProcessPendingProjectExportsUseCase(
            ProjectExportRepositoryPort exports,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            MediaFileRepositoryPort mediaFiles,
            MediaContentStoragePort storage,
            ProjectAudioMixerPort mixer,
            ProjectLayerArchivePort archiver,
            PublishedShortPort publishedShorts,
            Clock clock,
            int batchSize
    ) {
        this(exports, tracks, clips, mediaFiles, storage, mixer, archiver, publishedShorts,
                clock, batchSize, TransactionRunner.direct());
    }

    public ProcessPendingProjectExportsUseCase(
            ProjectExportRepositoryPort exports,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            MediaFileRepositoryPort mediaFiles,
            MediaContentStoragePort storage,
            ProjectAudioMixerPort mixer,
            ProjectLayerArchivePort archiver,
            PublishedShortPort publishedShorts,
            Clock clock,
            int batchSize,
            TransactionRunner transactionRunner
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.exports = exports;
        this.tracks = tracks;
        this.clips = clips;
        this.mediaFiles = mediaFiles;
        this.storage = storage;
        this.mixer = mixer;
        this.archiver = archiver;
        this.publishedShorts = publishedShorts;
        this.clock = clock;
        this.batchSize = batchSize;
        this.transactionRunner = transactionRunner;
    }

    public ProjectExportProcessingResult execute() {
        List<Long> exportIds = transactionRunner.readOnly(() -> exports.findQueuedExportIds(batchSize));
        int completed = 0;
        int failed = 0;
        int skipped = 0;
        for (Long exportId : exportIds) {
            try {
                ClaimedExport claimed = claim(exportId);
                if (claimed == null) {
                    skipped++;
                    continue;
                }
                process(claimed);
                completed++;
            } catch (RuntimeException exception) {
                markFailed(exportId, exception);
                failed++;
            }
        }
        return new ProjectExportProcessingResult(exportIds.size(), completed, failed, skipped);
    }

    private ClaimedExport claim(Long exportId) {
        return transactionRunner.required(() -> exports.findExportByIdForUpdate(exportId)
                .filter(export -> export.getStatus() == ExportStatus.QUEUED)
                .map(export -> {
                    export.startProcessing();
                    exports.save(export);
                    return new ClaimedExport(
                            export.getId(),
                            export.getProject().getId(),
                            export.getUser().getId(),
                            export.getProject().getMaxDurationMs());
                })
                .orElse(null));
    }

    private void process(ClaimedExport claimed) {
        List<ProjectTrack> approvedTracks = transactionRunner.readOnly(() ->
                tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(
                        claimed.projectId(), ApprovalStatus.APPROVED));
        boolean hasSoloTrack = approvedTracks.stream()
                .anyMatch(track -> track.isSolo() && !track.isMuted());
        List<AudioLayer> audioLayers = new ArrayList<>();
        List<Layer> archiveLayers = new ArrayList<>();
        Map<Long, StoredAudio> sourceCache = new HashMap<>();

        for (ProjectTrack track : approvedTracks) {
            List<ProjectClip> approvedClips = transactionRunner.readOnly(() ->
                    clips.findByTrackIdAndApprovalStatusAndIsDeletedFalseOrderByStartTickAscIdAsc(
                            track.getId(), ApprovalStatus.APPROVED));
            addArchiveLayers(track, approvedClips, archiveLayers, sourceCache);
            if (track.isMuted() || (hasSoloTrack && !track.isSolo())) {
                continue;
            }
            addAudioLayers(claimed, track, approvedClips, audioLayers, sourceCache);
        }
        if (archiveLayers.isEmpty()) {
            throw new IllegalStateException("압축할 승인된 프로젝트 레이어가 없습니다.");
        }
        if (audioLayers.isEmpty()) {
            throw new IllegalStateException("믹싱할 활성 오디오 레이어가 없습니다.");
        }

        byte[] archive = archiver.archive(archiveLayers);
        ProjectAudioMixerPort.MixedAudio mixed = mixer.mix(audioLayers);
        storeAndComplete(claimed, mixed, archive);
    }

    private void addArchiveLayers(
            ProjectTrack track,
            List<ProjectClip> approvedClips,
            List<Layer> archiveLayers,
            Map<Long, StoredAudio> sourceCache
    ) {
        for (ProjectClip clip : approvedClips) {
            if (clip.getClipType() == ClipType.AUDIO) {
                StoredAudio source = sourceCache.computeIfAbsent(
                        clip.getMediaFileId(), this::loadAudio);
                archiveLayers.add(new Layer(
                        track.getId(), clip.getId(), clip.getClipType(),
                        source.originalFilename(), source.content(), List.of()));
            } else {
                archiveLayers.add(new Layer(
                        track.getId(), clip.getId(), clip.getClipType(),
                        null, null, clip.getMidiNotes()));
            }
        }
    }

    private void addAudioLayers(
            ClaimedExport claimed,
            ProjectTrack track,
            List<ProjectClip> approvedClips,
            List<AudioLayer> audioLayers,
            Map<Long, StoredAudio> sourceCache
    ) {
        if (track.getMediaFileId() != null) {
            StoredAudio renderedTrack = sourceCache.computeIfAbsent(
                    track.getMediaFileId(), this::loadAudio);
            audioLayers.add(new AudioLayer(
                    renderedTrack.content(), 0, 0, claimed.maxDurationMs(),
                    track.getVolume(), track.getPan()));
            return;
        }
        if (approvedClips.stream().anyMatch(clip -> clip.getClipType() == ClipType.MIDI)) {
            throw new IllegalStateException(
                    "MIDI 트랙 렌더링 결과가 없습니다. SoundFont 렌더러를 구성한 뒤 다시 시도해 주세요.");
        }
        for (ProjectClip clip : approvedClips) {
            StoredAudio source = sourceCache.computeIfAbsent(
                    clip.getMediaFileId(), this::loadAudio);
            audioLayers.add(new AudioLayer(
                    source.content(), clip.getStartTimeMs(), clip.getClipOffsetMs(),
                    clip.getDurationMs(), track.getVolume(), track.getPan()));
        }
    }

    private StoredAudio loadAudio(Long mediaFileId) {
        MediaFile media = mediaFiles.findById(mediaFileId)
                .orElseThrow(() -> new IllegalStateException("오디오 미디어 파일을 찾을 수 없습니다."));
        if (media.getStatus() != MediaFileStatus.READY || media.getFileType() != MediaFileType.AUDIO) {
            throw new IllegalStateException("준비된 오디오 미디어 파일만 Export할 수 있습니다.");
        }
        return new StoredAudio(storage.get(media.getStorageKey()), media.getOriginalFilename());
    }

    private void storeAndComplete(
            ClaimedExport claimed,
            ProjectAudioMixerPort.MixedAudio mixed,
            byte[] archive
    ) {
        String baseKey = "exports/" + claimed.projectId() + "/" + claimed.exportId();
        String mixedAudioKey = baseKey + "/mix.wav";
        String archiveKey = baseKey + "/layers.zip";
        storage.put(mixedAudioKey, mixed.content(), MIXED_AUDIO_MIME_TYPE);
        storage.put(archiveKey, archive, ARCHIVE_MIME_TYPE);

        transactionRunner.required(() -> {
            MediaFile mixedAudio = mediaFiles.save(MediaFile.readyGenerated(
                    claimed.ownerId(), storage.publicUrl(mixedAudioKey), mixedAudioKey,
                    "mix.wav", MediaFileType.AUDIO, MIXED_AUDIO_MIME_TYPE, mixed.content().length));
            MediaFile layerArchive = mediaFiles.save(MediaFile.readyGenerated(
                    claimed.ownerId(), storage.publicUrl(archiveKey), archiveKey,
                    "layers.zip", MediaFileType.ARCHIVE, ARCHIVE_MIME_TYPE, archive.length));
            var export = exports.findExportByIdForUpdate(claimed.exportId())
                    .orElseThrow(() -> new IllegalStateException("처리 중인 Export를 찾을 수 없습니다."));
            Long shortId = publishedShorts.publish(
                    export.getProject(), export.getId(), mixedAudio.getId());
            export.complete(
                    mixedAudio.getId(), layerArchive.getId(), shortId, mixed.durationMs(), now());
            exports.save(export);
            return null;
        });
    }

    private void markFailed(Long exportId, RuntimeException exception) {
        transactionRunner.required(() -> {
            exports.findExportByIdForUpdate(exportId)
                    .filter(export -> export.getStatus() == ExportStatus.PROCESSING)
                    .ifPresent(export -> {
                        export.fail(failureMessage(exception), now());
                        exports.save(export);
                    });
            return null;
        });
    }

    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= 2_000 ? message : message.substring(0, 2_000);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private record ClaimedExport(Long exportId, Long projectId, Long ownerId, int maxDurationMs) {
    }

    private record StoredAudio(byte[] content, String originalFilename) {
    }
}
