package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import glitched.adlips.application.project.dto.request.TrackGetListRequest;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.TrackGetListUseCase;
import glitched.adlips.application.user.profile.port.out.*;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;

class TrackVisibilityUseCaseTest {
    final User owner = user(1), approved = user(2), piano = user(3), bass = user(4);
    final Project project = new Project(owner, "곡", "설명", 1L);
    final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    final ProjectContributionRepositoryPort contributions = mock(ProjectContributionRepositoryPort.class);
    final List<ProjectTrack> all = new ArrayList<>();
    final Map<Long, ProjectClip> clipById = new HashMap<>();

    TrackVisibilityUseCaseTest() {
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        for (User user : List.of(owner, approved, piano, bass))
            when(members.findByProjectIdAndUserId(10L, user.getId())).thenReturn(Optional.of(
                    new ProjectMember(project, user, user == owner ? ProjectMemberRole.OWNER : ProjectMemberRole.EDITOR)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(all);
        track(10, owner, ApprovalStatus.APPROVED, 100, ApprovalStatus.APPROVED);
        track(11, approved, ApprovalStatus.APPROVED, 110, ApprovalStatus.APPROVED);
        track(12, piano, ApprovalStatus.PENDING, 120, ApprovalStatus.PENDING);
        track(13, bass, ApprovalStatus.PENDING, 130, ApprovalStatus.PENDING);
        track(14, bass, ApprovalStatus.REJECTED, 140, ApprovalStatus.REJECTED);
    }

    @Test
    void ordinaryStudioDoesNotExposeOtherPeoplesPendingOrRejectedLayers() {
        var response = useCase().execute(new TrackGetListRequest(10L, owner.getId()));
        assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L);
    }


    @Test
    void approvedViewHidesEvenTheViewersOwnPendingLayerAndRestrictsEditing() {
        var response = useCase().execute(new TrackGetListRequest(10L, piano.getId(), TrackGetListRequest.View.APPROVED, null));
        assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L);
        assertThat(response.viewerPermission().canEdit()).isFalse();
        assertThat(response.viewerPermission().canContribute()).isFalse();
    }

    @Test
    void aRequestShowsApprovedLayersAndOnlyItsExactTargets() {
        request(200, piano, 12, 120);
        extraClip(12, 121, piano, ApprovalStatus.DRAFT);
        for (User viewer : List.of(owner, piano)) {
            var response = useCase().execute(new TrackGetListRequest(10L, viewer.getId(), TrackGetListRequest.View.WORKING, 200L));
            assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L, 12L);
            assertThat(response.tracks().getLast().clips()).extracting(c -> c.clipId()).containsExactly(120L);
            assertThat(response.viewerPermission().canEdit()).isFalse();
        }
    }

    @Test
    void otherRequestsAndOtherProjectsCannotBeReadByGuessingAnId() {
        request(200, piano, 12, 120);
        assertThatThrownBy(() -> useCase().execute(new TrackGetListRequest(10L, bass.getId(), TrackGetListRequest.View.APPROVED, 200L)))
                .isInstanceOfSatisfying(ProjectApplicationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED));
        assertThatThrownBy(() -> useCase().execute(new TrackGetListRequest(10L, owner.getId(), TrackGetListRequest.View.APPROVED, 999L)))
                .isInstanceOfSatisfying(ProjectApplicationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ProjectErrorCode.CONTRIBUTION_NOT_FOUND));
    }

    @Test
    void clipOnlyRequestOnAnApprovedTrackDoesNotExposeOtherUnapprovedClips() {
        extraClip(11, 111, approved, ApprovalStatus.PENDING);
        extraClip(11, 112, approved, ApprovalStatus.DRAFT);
        var contribution = new ProjectContribution(project, approved, "클립 추가");
        new ProjectContributionItem(contribution, ContributionChangeType.ADD, ContributionTargetType.CLIP, 111L);
        when(contributions.findByIdAndProjectId(201L, 10L)).thenReturn(Optional.of(contribution));
        var response = useCase().execute(new TrackGetListRequest(10L, owner.getId(), TrackGetListRequest.View.APPROVED, 201L));
        assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L);
        assertThat(response.tracks().getLast().clips()).extracting(c -> c.clipId()).containsExactly(110L, 111L);
        var ordinary = useCase().execute(new TrackGetListRequest(10L, owner.getId(), TrackGetListRequest.View.APPROVED, null));
        assertThat(ordinary.tracks().getLast().clips()).extracting(c -> c.clipId()).containsExactly(110L);
    }

    @Test
    void workingCopyKeepsOwnPendingAndDraftNotesButNotSomeoneElsesRequest() {
        extraClip(12, 121, piano, ApprovalStatus.DRAFT);
        var response = useCase().execute(new TrackGetListRequest(10L, piano.getId()));
        assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L, 12L);
        assertThat(response.tracks().getLast().clips()).extracting(c -> c.clipId()).containsExactly(120L, 121L);
    }

    @Test
    void approvedContributionBecomesVisibleInTheOrdinaryStudio() {
        when(all.get(2).getApprovalStatus()).thenReturn(ApprovalStatus.APPROVED);
        when(clipById.get(120L).getApprovalStatus()).thenReturn(ApprovalStatus.APPROVED);
        var response = useCase().execute(new TrackGetListRequest(10L, owner.getId(), TrackGetListRequest.View.APPROVED, null));
        assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L, 12L);
    }

    @Test
    void normalStudioKeepsPrivateWorkEditableButPublishedStudioIsApprovedOnly() {
        var privateWork = useCase().execute(new TrackGetListRequest(10L, piano.getId(), TrackGetListRequest.View.STUDIO, null));
        assertThat(privateWork.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L, 12L);
        assertThat(privateWork.viewerPermission().canEdit()).isTrue();
        project.startPublishing();
        project.publish(java.time.LocalDateTime.now());
        var publicStudio = useCase().execute(new TrackGetListRequest(10L, piano.getId(), TrackGetListRequest.View.STUDIO, null));
        assertThat(publicStudio.tracks()).extracting(t -> t.trackId()).containsExactly(10L, 11L);
        assertThat(publicStudio.viewerPermission().canEdit()).isFalse();
    }

    void request(long id, User user, long trackId, long clipId) {
        var contribution = new ProjectContribution(project, user, "새 레이어");
        new ProjectContributionItem(contribution, ContributionChangeType.ADD, ContributionTargetType.TRACK, trackId);
        new ProjectContributionItem(contribution, ContributionChangeType.ADD, ContributionTargetType.CLIP, clipId);
        when(contributions.findByIdAndProjectId(id, 10L)).thenReturn(Optional.of(contribution));
    }

    void extraClip(long trackId, long id, User user, ApprovalStatus status) {
        ProjectClip clip = mock(ProjectClip.class);
        when(clip.getId()).thenReturn(id);
        when(clip.getOwner()).thenReturn(user);
        when(clip.getApprovalStatus()).thenReturn(status);
        clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(trackId).add(clip);
    }

    TrackGetListUseCase useCase() {
        return new TrackGetListUseCase(projects, members, tracks, clips,
                mock(MediaFileRepositoryPort.class), mock(ProfileRepositoryPort.class), contributions);
    }

    ProjectTrack track(long id, User user, ApprovalStatus status, long clipId, ApprovalStatus clipStatus) {
        ProjectTrack track = mock(ProjectTrack.class);
        when(track.getId()).thenReturn(id);
        when(track.getOwner()).thenReturn(user);
        when(track.getApprovalStatus()).thenReturn(status);
        ProjectClip clip = mock(ProjectClip.class);
        when(clip.getId()).thenReturn(clipId);
        when(clip.getOwner()).thenReturn(user);
        when(clip.getApprovalStatus()).thenReturn(clipStatus);
        when(clip.getClipType()).thenReturn(ClipType.MIDI);
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(id)).thenReturn(new ArrayList<>(List.of(clip)));
        all.add(track);
        clipById.put(clipId, clip);
        return track;
    }

    static User user(long id) { return User.create("user" + id + "@test.local").withId(id); }
}
