package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.notification.NotificationEvents;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.ProjectContributionReviewRequest;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.project.ProjectExport;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectContributionReviewUseCaseTest {
    @Test
    void approvesAddContributionAndIncreasesMajorVersion() {
        Fixture fixture = new Fixture();

        var response = fixture.useCase.execute(new ProjectContributionReviewRequest(
                1L, 10L, 1L, ContributionApprovalStatus.APPROVED, "좋습니다."));

        assertThat(response.approvalStatus()).isEqualTo(ContributionApprovalStatus.APPROVED);
        assertThat(response.previousProjectVersion().display()).isEqualTo("v1.1");
        assertThat(response.projectVersion().display()).isEqualTo("v2.1");
        assertThat(fixture.track.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        verify(fixture.exports).save(any(ProjectExport.class));
    }

    @Test
    void rejectsContributionWithoutChangingProjectVersion() {
        Fixture fixture = new Fixture();

        var response = fixture.useCase.execute(new ProjectContributionReviewRequest(
                1L, 10L, 1L, ContributionApprovalStatus.REJECTED, "방향이 맞지 않습니다."));

        assertThat(response.projectVersion().display()).isEqualTo("v1.1");
        assertThat(fixture.track.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        verifyNoInteractions(fixture.exports);
    }

    @Test
    void rejectsApprovingDuplicateSampleTrackAndLocksProject() {
        User reviewer = User.create("owner@example.com").withId(1L);
        User contributor = User.create("contributor@example.com").withId(2L);
        Project project = new Project(reviewer, "곡", null, 701L);
        project.startPublishing();
        project.publish(LocalDateTime.of(2026, 9, 29, 23, 0));

        ProjectTrack existingApprovedSample = new ProjectTrack(project, reviewer, "Sample", "drum-kit", 0, 55L);
        ReflectionTestUtils.setField(existingApprovedSample, "id", 100L);
        existingApprovedSample.approve();

        ProjectTrack pendingDuplicateSample = new ProjectTrack(project, contributor, "Sample", "drum-kit", 1, 55L);
        ReflectionTestUtils.setField(pendingDuplicateSample, "id", 102L);
        pendingDuplicateSample.submitForReview();

        ProjectContribution contribution = new ProjectContribution(project, contributor, "샘플 추가");
        new ProjectContributionItem(contribution, ContributionChangeType.ADD,
                ContributionTargetType.TRACK, 102L);

        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectContributionRepositoryPort contributions = mock(ProjectContributionRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProjectExportRepositoryPort exports = mock(ProjectExportRepositoryPort.class);

        when(projects.findByIdAndDeletedAtIsNullForUpdate(1L)).thenReturn(Optional.of(project));
        when(contributions.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(contribution));
        when(members.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, reviewer, ProjectMemberRole.OWNER)));
        when(users.findById(1L)).thenReturn(Optional.of(reviewer));
        when(tracks.findTrackByIdAndIsDeletedFalse(102L)).thenReturn(Optional.of(pendingDuplicateSample));
        when(tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(1L, ApprovalStatus.APPROVED))
                .thenReturn(List.of(existingApprovedSample));

        ProjectContributionReviewUseCase useCase = new ProjectContributionReviewUseCase(
                projects, contributions, members, tracks, clips, users, exports,
                TransactionRunner.direct(), NotificationEvents.none());

        assertThatThrownBy(() -> useCase.execute(new ProjectContributionReviewRequest(
                1L, 10L, 1L, ContributionApprovalStatus.APPROVED, "승인 시도")))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.TRACK_LIMIT_EXCEEDED);

        verify(projects).findByIdAndDeletedAtIsNullForUpdate(1L);
        assertThat(project.getMajorVersion()).isEqualTo(1);
        assertThat(project.getMinorVersion()).isEqualTo(1);
        assertThat(contribution.getApprovalStatus()).isEqualTo(ContributionApprovalStatus.PENDING);
        assertThat(pendingDuplicateSample.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        verifyNoInteractions(exports);
    }

    @Test
    void rejectsApprovingASampleClipOrSampleTrackWithoutItsPair() {
        SampleFixture clipOnly = new SampleFixture();
        clipOnly.item(ContributionChangeType.ADD, ContributionTargetType.CLIP, 202L);

        assertThatThrownBy(clipOnly::approve)
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM);
        assertThat(clipOnly.clip.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(clipOnly.contribution.getApprovalStatus()).isEqualTo(ContributionApprovalStatus.PENDING);

        SampleFixture trackOnly = new SampleFixture();
        trackOnly.item(ContributionChangeType.ADD, ContributionTargetType.TRACK, 102L);

        assertThatThrownBy(trackOnly::approve)
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM);
        assertThat(trackOnly.track.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(trackOnly.contribution.getApprovalStatus()).isEqualTo(ContributionApprovalStatus.PENDING);
    }

    @Test
    void approvesASampleTrackTogetherWithItsSourceClip() {
        SampleFixture fixture = new SampleFixture();
        fixture.item(ContributionChangeType.ADD, ContributionTargetType.TRACK, 102L);
        fixture.item(ContributionChangeType.ADD, ContributionTargetType.CLIP, 202L);

        fixture.approve();

        assertThat(fixture.track.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(fixture.clip.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void approvingSourceClipDeletionAlsoDeletesTheDedicatedSampleTrack() {
        SampleFixture fixture = new SampleFixture();
        fixture.track.approve();
        fixture.clip.approve();
        fixture.item(ContributionChangeType.DELETE, ContributionTargetType.CLIP, 202L);

        fixture.approve();

        assertThat(fixture.clip.isDeleted()).isTrue();
        assertThat(fixture.track.isDeleted()).isTrue();
    }

    private static class SampleFixture {
        private final User reviewer = User.create("owner@example.com").withId(1L);
        private final User contributor = User.create("contributor@example.com").withId(2L);
        private final Project project = new Project(reviewer, "곡", null, 701L);
        private final ProjectTrack track = new ProjectTrack(project, contributor, "Sample", "drum-kit", 1, 55L);
        private final ProjectClip clip = ProjectClip.createAudio(
                project, track, contributor, ClipSourceType.IMPORTED, 21L, null, 0, 960, 0, 500, 0);
        private final ProjectContribution contribution = new ProjectContribution(project, contributor, "샘플");
        private final ProjectContributionReviewUseCase useCase;

        private SampleFixture() {
            ReflectionTestUtils.setField(track, "id", 102L);
            ReflectionTestUtils.setField(clip, "id", 202L);
            track.submitForReview();
            clip.submitForReview();
            ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
            ProjectContributionRepositoryPort contributions = mock(ProjectContributionRepositoryPort.class);
            ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
            ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
            ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
            UserRepositoryPort users = mock(UserRepositoryPort.class);
            when(projects.findByIdAndDeletedAtIsNullForUpdate(1L)).thenReturn(Optional.of(project));
            when(contributions.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(contribution));
            when(members.findByProjectIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(new ProjectMember(project, reviewer, ProjectMemberRole.OWNER)));
            when(users.findById(1L)).thenReturn(Optional.of(reviewer));
            when(tracks.findTrackByIdAndIsDeletedFalse(102L)).thenReturn(Optional.of(track));
            when(clips.findClipByIdAndIsDeletedFalse(202L)).thenReturn(Optional.of(clip));
            when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(102L)).thenReturn(List.of(clip));
            useCase = new ProjectContributionReviewUseCase(projects, contributions, members, tracks, clips, users,
                    mock(ProjectExportRepositoryPort.class), TransactionRunner.direct(), NotificationEvents.none());
        }

        private void item(ContributionChangeType changeType, ContributionTargetType targetType, Long targetId) {
            new ProjectContributionItem(contribution, changeType, targetType, targetId);
        }

        private void approve() {
            useCase.execute(new ProjectContributionReviewRequest(
                    1L, 10L, 1L, ContributionApprovalStatus.APPROVED, null));
        }
    }

    private static class Fixture {
        private final User reviewer = User.create("owner@example.com").withId(1L);
        private final User contributor = User.create("contributor@example.com").withId(2L);
        private final Project project = new Project(reviewer, "곡", null, 701L);
        private final ProjectTrack track = new ProjectTrack(project, contributor, "Guitar", "GUITAR", 0);
        private final ProjectContribution contribution = new ProjectContribution(project, contributor, "리프 추가");
        private final ProjectExportRepositoryPort exports = mock(ProjectExportRepositoryPort.class);
        private final ProjectContributionReviewUseCase useCase;

        private Fixture() {
            project.startPublishing();
            project.publish(LocalDateTime.of(2026, 9, 29, 23, 0));
            track.submitForReview();
            new ProjectContributionItem(contribution, ContributionChangeType.ADD,
                    ContributionTargetType.TRACK, 101L);
        ProjectContributionRepositoryPort contributions = mock(ProjectContributionRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
            UserRepositoryPort users = mock(UserRepositoryPort.class);
            when(contributions.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(contribution));
            when(members.findByProjectIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(new ProjectMember(project, reviewer, ProjectMemberRole.OWNER)));
            when(users.findById(1L)).thenReturn(Optional.of(reviewer));
            when(tracks.findTrackByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(track));
            useCase = new ProjectContributionReviewUseCase(
                    contributions, members, tracks, clips, users, exports);
        }
    }
}
