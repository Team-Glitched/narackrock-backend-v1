package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.project.port.out.ProjectAudioMixerPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.usecase.ProcessPendingProjectExportsUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ExportStatus;
import glitched.adlips.domain.project.MidiNote;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectExport;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProcessPendingProjectExportsUseCaseTest {

    private final ProjectExportRepositoryPort exports = mock(ProjectExportRepositoryPort.class);
    private final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    private final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    private final MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
    private final MediaContentStoragePort storage = mock(MediaContentStoragePort.class);
    private final ProjectAudioMixerPort mixer = mock(ProjectAudioMixerPort.class);
    private final ProjectLayerArchivePort archiver = mock(ProjectLayerArchivePort.class);
    private final PublishedShortPort publishedShorts = mock(PublishedShortPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T01:00:00Z"), ZoneOffset.UTC);

    private User owner;
    private Project project;
    private ProjectExport export;

    @BeforeEach
    void setUp() {
        owner = User.create("owner@example.com").withId(1L);
        project = new Project(owner, "곡", null, 701L);
        ReflectionTestUtils.setField(project, "id", 10L);
        project.startPublishing();
        export = new ProjectExport(project, owner);
        ReflectionTestUtils.setField(export, "id", 20L);
        when(exports.findQueuedExportIds(10)).thenReturn(List.of(20L));
        when(exports.findExportByIdForUpdate(20L)).thenReturn(Optional.of(export));
        when(exports.save(any(ProjectExport.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void mixesApprovedAudioAndStoresWaveAndLayerArchive() {
        ProjectTrack track = approvedTrack(11L, "기타");
        ProjectClip clip = ProjectClip.createAudio(
                project, track, owner, ClipSourceType.FILE_UPLOAD,
                301L, null, 0, 1_920, 0, 2_000, 0);
        clip.approve();
        ReflectionTestUtils.setField(clip, "id", 21L);
        when(tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(10L, ApprovalStatus.APPROVED))
                .thenReturn(List.of(track));
        when(clips.findByTrackIdAndApprovalStatusAndIsDeletedFalseOrderByStartTickAscIdAsc(
                11L, ApprovalStatus.APPROVED)).thenReturn(List.of(clip));
        when(mediaFiles.findById(301L)).thenReturn(Optional.of(readyAudio(301L, "audio/1/guitar.wav")));
        when(storage.get("audio/1/guitar.wav")).thenReturn(new byte[]{1, 2, 3});
        when(storage.publicUrl(any())).thenAnswer(invocation -> "/files/" + invocation.getArgument(0));
        when(mixer.mix(any())).thenReturn(new ProjectAudioMixerPort.MixedAudio(new byte[]{4, 5}, 2_000));
        when(archiver.archive(any())).thenReturn(new byte[]{6, 7, 8});
        AtomicLong generatedId = new AtomicLong(800L);
        when(mediaFiles.save(any(MediaFile.class)))
                .thenAnswer(invocation -> invocation.<MediaFile>getArgument(0).withId(generatedId.incrementAndGet()));
        when(publishedShorts.publish(project, 20L, 801L)).thenReturn(901L);

        var result = useCase().execute();

        assertThat(result.completedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(export.getStatus()).isEqualTo(ExportStatus.COMPLETED);
        assertThat(export.getMediaFileId()).isEqualTo(801L);
        assertThat(export.getLayerArchiveFileId()).isEqualTo(802L);
        assertThat(export.getShortId()).isEqualTo(901L);
        verify(storage).put("exports/10/20/mix.wav", new byte[]{4, 5}, "audio/wav");
        verify(storage).put("exports/10/20/layers.zip", new byte[]{6, 7, 8}, "application/zip");
    }

    @Test
    void marksExportFailedWhenMidiHasNoRenderedTrackAudio() {
        ProjectTrack track = approvedTrack(12L, "피아노");
        ProjectClip clip = ProjectClip.createMidi(
                project, track, owner, 0, 1_920,
                List.of(new MidiNote(60, 0, 480, 100, null, Map.of())), 0);
        clip.approve();
        ReflectionTestUtils.setField(clip, "id", 22L);
        when(tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(10L, ApprovalStatus.APPROVED))
                .thenReturn(List.of(track));
        when(clips.findByTrackIdAndApprovalStatusAndIsDeletedFalseOrderByStartTickAscIdAsc(
                12L, ApprovalStatus.APPROVED)).thenReturn(List.of(clip));

        var result = useCase().execute();

        assertThat(result.completedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(export.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(export.getErrorMessage()).contains("MIDI");
        verify(mixer, never()).mix(any());
    }

    private ProcessPendingProjectExportsUseCase useCase() {
        return new ProcessPendingProjectExportsUseCase(
                exports, tracks, clips, mediaFiles, storage, mixer, archiver,
                publishedShorts, clock, 10);
    }

    private ProjectTrack approvedTrack(Long id, String instrument) {
        ProjectTrack track = new ProjectTrack(project, owner, "트랙", instrument, 0);
        track.approve();
        ReflectionTestUtils.setField(track, "id", id);
        return track;
    }

    private MediaFile readyAudio(Long id, String key) {
        return MediaFile.restore(id, 1L, "/files/" + key, key, "guitar.wav",
                MediaFileType.AUDIO, "audio/wav", 3L, MediaFileStatus.READY);
    }
}
