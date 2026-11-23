package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.dto.response.TrackCreateResponse;
import glitched.adlips.application.project.usecase.TrackCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TrackCreateUseCaseTest {
    @Test
    void createsDraftTrackWithoutRenderedMedia() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(tracks.save(any(ProjectTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrackCreateResponse response = new TrackCreateUseCase(projects, members, tracks, users)
                .execute(new TrackCreateRequest(10L, 1L, "Guitar Track", "GUITAR", 0));

        assertThat(response.volume()).isEqualTo(100);
        assertThat(response.mediaFileId()).isNull();
        assertThat(response.approvalStatus().name()).isEqualTo("DRAFT");
    }

    @Test
    void rejectsASecondActiveTrackFromTheSameUserInAProject() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(tracks.existsByProjectIdAndOwnerIdAndOriginOpenSourceIdIsNullAndIsDeletedFalse(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> new TrackCreateUseCase(projects, members, tracks, users)
                .execute(new TrackCreateRequest(10L, 1L, "두 번째 레이어", "PIANO", 1)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.TRACK_LIMIT_EXCEEDED);
        verify(tracks, never()).save(any(ProjectTrack.class));
    }

    @ParameterizedTest
    @CsvSource({
        "electric-guitar, electric-guitar, DRAFT",
        "electric-guitar, distortion-guitar, PENDING",
        "nylon-guitar, acoustic-guitar, APPROVED",
        "vocal, vocal, APPROVED",
        "vocal, backing-vocal, PENDING",
        "backing-vocal, rap, DRAFT"
    })
    void allowsAnotherPlayerToChooseTheSameInstrumentFamily(String existing, String selected, ApprovalStatus status) {
        User owner = User.create("owner@example.com").withId(1L);
        User editor = User.create("editor@example.com").withId(2L);
        Project project = new Project(owner, "곡", null, 0L);
        var projects=mock(ProjectRepositoryPort.class);var members=mock(ProjectMemberRepositoryPort.class);
        var tracks=mock(ProjectTrackRepositoryPort.class);var users=mock(UserRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L,2L)).thenReturn(Optional.of(new ProjectMember(project,editor,ProjectMemberRole.EDITOR)));
        when(users.findById(2L)).thenReturn(Optional.of(editor));
        var teammate = new ProjectTrack(project, owner, "팀원 레이어", existing, 0);
        if (status == ApprovalStatus.PENDING) teammate.submitForReview();
        if (status == ApprovalStatus.APPROVED) teammate.approve();
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(java.util.List.of(teammate));
        when(tracks.save(any(ProjectTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new TrackCreateUseCase(projects, members, tracks, users)
            .execute(new TrackCreateRequest(10L, 2L, "내 레이어", selected, 1));

        assertThat(response.instrument()).isEqualTo(selected);
        assertThat(teammate.getInstrument()).isEqualTo(existing);
        verify(tracks).save(any(ProjectTrack.class));
    }
}
