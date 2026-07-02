package glitched.adlips.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectCompositionDomainTest {

    private final User owner = User.create("owner@example.com");

    @Test
    void calculatesMaxTickFromDurationBpmAndPpq() {
        Project project = project("곡");

        assertThat(project.getMaxTick()).isEqualTo(57_600L);
        assertThat(project.tickToMilliseconds(1_920)).isEqualTo(2_000);
    }

    @Test
    void incrementsProjectVersionByMergeAndDirectModificationRules() {
        Project project = project("곡");

        project.increaseMinorVersion();
        assertThat(project.getDisplayVersion()).isEqualTo("v1.2");

        project.increaseMajorVersion();
        assertThat(project.getDisplayVersion()).isEqualTo("v2.1");
    }

    @Test
    void createsAnAudioClipWithMediaAndOffset() {
        Project project = project("곡");
        ProjectTrack track = new ProjectTrack(project, owner, "기타", "GUITAR", 0);

        ProjectClip clip = ProjectClip.createAudio(
                project, track, owner, ClipSourceType.RECORDING,
                501L, 502L, 1_920, 960, 250, 2_000, 0);

        assertThat(clip.getMediaFileId()).isEqualTo(501L);
        assertThat(clip.getWaveformFileId()).isEqualTo(502L);
        assertThat(clip.getClipOffsetMs()).isEqualTo(250);
        assertThat(clip.getStartTimeMs()).isEqualTo(2_000);
        assertThat(clip.getDurationMs()).isEqualTo(2_000);
        assertThat(clip.getMidiNotes()).isEmpty();
    }

    @Test
    void updatesAnAudioClipWithMediaTimelineAndOffset() {
        Project project = project("곡");
        ProjectTrack track = new ProjectTrack(project, owner, "기타", "GUITAR", 0);
        ProjectClip clip = ProjectClip.createAudio(
                project, track, owner, ClipSourceType.RECORDING,
                501L, 502L, 0, 1_920, 0, 2_000, 0);
        track.replaceRenderedMedia(601L);

        clip.updateAudio(
                ClipSourceType.FILE_UPLOAD, 503L, 504L,
                960, 1_920, 250, 2_000);

        assertThat(clip.getSourceType()).isEqualTo(ClipSourceType.FILE_UPLOAD);
        assertThat(clip.getMediaFileId()).isEqualTo(503L);
        assertThat(clip.getWaveformFileId()).isEqualTo(504L);
        assertThat(clip.getStartTick()).isEqualTo(960);
        assertThat(clip.getClipOffsetMs()).isEqualTo(250);
        assertThat(track.getMediaFileId()).isNull();
    }

    @Test
    void rejectsMissingAudioMediaAndMidiOnATrackWithoutInstrument() {
        Project project = project("곡");
        ProjectTrack audioTrack = new ProjectTrack(project, owner, "오디오", null, 0);
        ProjectTrack midiTrack = new ProjectTrack(project, owner, "MIDI", null, 1);

        assertThatThrownBy(() -> ProjectClip.createAudio(
                project, audioTrack, owner, ClipSourceType.RECORDING,
                null, null, 0, 1_920, 0, 2_000, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mediaFileId");
        assertThatThrownBy(() -> ProjectClip.createMidi(
                project, midiTrack, owner, 0, 1_920, List.of(), 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("instrument");
    }

    @Test
    void createsAMidiClipWithStructuredNotes() {
        Project project = project("곡");
        ProjectTrack track = new ProjectTrack(project, owner, "피아노", "PIANO", 0);
        MidiNote note = new MidiNote(60, 0, 480, 100, "DELAY", Map.of("delayMs", 250));

        ProjectClip clip = ProjectClip.createMidi(
                project, track, owner, 0, 1_920, List.of(note), 0);

        assertThat(clip.getClipType()).isEqualTo(ClipType.MIDI);
        assertThat(clip.getSourceType()).isEqualTo(ClipSourceType.MIDI_INPUT);
        assertThat(clip.getMidiNotes()).containsExactly(note);
        assertThat(clip.getMediaFileId()).isNull();
    }

    @Test
    void invalidatesRenderedTrackMediaWhenAClipChanges() {
        Project project = project("곡");
        ProjectTrack track = new ProjectTrack(project, owner, "피아노", "PIANO", 0);
        track.replaceRenderedMedia(601L);

        ProjectClip.createMidi(project, track, owner, 0, 1_920, List.of(), 0);

        assertThat(track.getMediaFileId()).isNull();
    }

    @Test
    void rejectsAClipWhoseTrackBelongsToAnotherProject() {
        Project project = project("원본");
        Project otherProject = project("다른 곡");
        ProjectTrack otherTrack = new ProjectTrack(otherProject, owner, "기타", "GUITAR", 0);

        assertThatThrownBy(() -> ProjectClip.createMidi(
                project, otherTrack, owner, 0, 1_920, List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same project");
    }

    @Test
    void rejectsAClipOutsideTheProjectTimeline() {
        Project project = project("곡");
        ProjectTrack track = new ProjectTrack(project, owner, "피아노", "PIANO", 0);

        assertThatThrownBy(() -> ProjectClip.createMidi(
                project, track, owner, 57_000, 1_000, List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeline");
    }

    @Test
    void capturesProjectVersionWhenCreatingContributionAndExport() {
        Project project = project("곡");
        project.increaseMajorVersion();

        ProjectContribution contribution = new ProjectContribution(project, owner, "기타 추가");
        ProjectExport export = new ProjectExport(project, owner);

        assertThat(contribution.getBaseMajorVersion()).isEqualTo(2);
        assertThat(contribution.getBaseMinorVersion()).isEqualTo(1);
        assertThat(export.getProjectMajorVersion()).isEqualTo(2);
        assertThat(export.getProjectMinorVersion()).isEqualTo(1);
        assertThat(export.getStatus()).isEqualTo(ExportStatus.QUEUED);
        assertThat(export.getMediaFileId()).isNull();
    }

    @Test
    void approvesAnAddContributionEvenWhenItsBaseVersionIsOld() {
        Project project = project("곡");
        ProjectContribution contribution = new ProjectContribution(
                project, owner, "트랙 추가", 1, 1);
        new ProjectContributionItem(
                contribution, ContributionChangeType.ADD,
                ContributionTargetType.TRACK, 10L);
        project.increaseMinorVersion();
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 7, 2, 10, 0);

        contribution.approve(owner, "승인", reviewedAt);

        assertThat(contribution.getApprovalStatus()).isEqualTo(ContributionApprovalStatus.APPROVED);
        assertThat(contribution.getItems()).extracting(ProjectContributionItem::getItemStatus)
                .containsExactly(ContributionItemStatus.APPROVED);
        assertThat(project.getDisplayVersion()).isEqualTo("v2.1");
        assertThat(contribution.getReviewer()).isSameAs(owner);
        assertThat(contribution.getReviewedAt()).isEqualTo(reviewedAt);
    }

    @Test
    void rejectsApprovalOfAnUpdateContributionBasedOnAnOldProjectVersion() {
        Project project = project("곡");
        ProjectContribution contribution = new ProjectContribution(
                project, owner, "클립 수정", 1, 1);
        new ProjectContributionItem(
                contribution, ContributionChangeType.UPDATE,
                ContributionTargetType.CLIP, 20L);
        project.increaseMinorVersion();

        assertThatThrownBy(() -> contribution.approve(
                owner, "승인", LocalDateTime.of(2026, 7, 2, 10, 0)))
                .isInstanceOf(ContributionVersionConflictException.class);
        assertThat(contribution.getApprovalStatus()).isEqualTo(ContributionApprovalStatus.PENDING);
        assertThat(project.getDisplayVersion()).isEqualTo("v1.2");
    }

    @Test
    void rejectingAContributionDoesNotChangeTheProjectVersion() {
        Project project = project("곡");
        ProjectContribution contribution = new ProjectContribution(project, owner, "기타 추가");
        ProjectContributionItem item = new ProjectContributionItem(
                contribution, ContributionChangeType.ADD,
                ContributionTargetType.TRACK, 10L);

        contribution.reject(owner, "방향이 다름", LocalDateTime.of(2026, 7, 2, 10, 0));

        assertThat(contribution.getApprovalStatus()).isEqualTo(ContributionApprovalStatus.REJECTED);
        assertThat(item.getItemStatus()).isEqualTo(ContributionItemStatus.REJECTED);
        assertThat(project.getDisplayVersion()).isEqualTo("v1.1");
    }

    @Test
    void completesAnExportWithTheFinalMixedAudio() {
        ProjectExport export = new ProjectExport(project("곡"), owner);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 1, 18, 0);

        export.startProcessing();
        export.complete(9001L, 45_000, completedAt);

        assertThat(export.getStatus()).isEqualTo(ExportStatus.COMPLETED);
        assertThat(export.getMediaFileId()).isEqualTo(9001L);
        assertThat(export.getDurationMs()).isEqualTo(45_000);
        assertThat(export.getCompletedAt()).isEqualTo(completedAt);
    }

    private Project project(String title) {
        return new Project(owner, title, "설명", 701L);
    }
}
