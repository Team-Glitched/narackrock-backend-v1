package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectMemberJoinPort;
import glitched.adlips.application.project.usecase.ProjectMemberJoinUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ProjectMemberJoinUseCaseTest {

    @Test
    void 완료된_프로젝트에는_멤버로_참여할_수_없다() {
        ProjectMemberJoinPort members = mock(ProjectMemberJoinPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        Project project = mock(Project.class);
        when(project.getStatus()).thenReturn(ProjectStatus.COMPLETED);
        when(members.findActiveProjectForUpdate(8L)).thenReturn(Optional.of(project));
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(members, users);

        assertThatThrownBy(() -> useCase.execute(8L, 1L))
                .isInstanceOf(ProjectApplicationException.class)
                .hasMessage("완료된 프로젝트에는 참여할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);

        verify(members, never()).existsByProjectIdAndUserId(any(), any());
        verify(members, never()).saveEditor(any(), any());
        verify(users, never()).findById(any());
    }

    @Test
    void 프로젝트를_잠근_뒤_이미_멤버면_아무것도_하지_않는다() {
        ProjectMemberJoinPort members = mock(ProjectMemberJoinPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        Project project = mock(Project.class);
        when(members.findActiveProjectForUpdate(8L)).thenReturn(Optional.of(project));
        when(members.existsByProjectIdAndUserId(8L, 1L)).thenReturn(true);
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(members, users);

        useCase.execute(8L, 1L);

        InOrder order = inOrder(members);
        order.verify(members).findActiveProjectForUpdate(8L);
        order.verify(members).existsByProjectIdAndUserId(8L, 1L);
        verify(members, never()).saveEditor(any(), any());
    }

    @Test
    void 멤버가_아니면_EDITOR로_등록한다() {
        ProjectMemberJoinPort members = mock(ProjectMemberJoinPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        User owner = User.create("owner@example.com").withId(2L);
        Project project = new Project(owner, "밤하늘 위 멜로디", null, 701L);
        User joiner = User.create("joiner@example.com").withId(1L);
        when(members.findActiveProjectForUpdate(8L)).thenReturn(Optional.of(project));
        when(members.existsByProjectIdAndUserId(8L, 1L)).thenReturn(false);
        when(users.findById(1L)).thenReturn(Optional.of(joiner));
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(members, users);

        useCase.execute(8L, 1L);

        verify(members).saveEditor(project, joiner);
    }

    @Test
    void 프로젝트가_없거나_삭제됐으면_PROJECT_NOT_FOUND_예외가_발생한다() {
        ProjectMemberJoinPort members = mock(ProjectMemberJoinPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(members.findActiveProjectForUpdate(8L)).thenReturn(Optional.empty());
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(members, users);

        assertThatThrownBy(() -> useCase.execute(8L, 1L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND);

        verify(members, never()).saveEditor(any(), any());
    }

    @Test
    void 사용자가_없거나_비활성이면_PROJECT_ACCESS_DENIED_예외가_발생한다() {
        ProjectMemberJoinPort members = mock(ProjectMemberJoinPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        Project project = mock(Project.class);
        when(members.findActiveProjectForUpdate(8L)).thenReturn(Optional.of(project));
        when(members.existsByProjectIdAndUserId(8L, 1L)).thenReturn(false);
        when(users.findById(1L)).thenReturn(Optional.empty());
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(members, users);

        assertThatThrownBy(() -> useCase.execute(8L, 1L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);

        verify(members, never()).saveEditor(any(), any());
    }
}
