package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.usecase.ProjectMemberJoinUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectMemberJoinUseCaseTest {

    @Test
    void 이미_멤버면_아무것도_하지_않는다() {
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(members.findByProjectIdAndUserId(8L, 1L)).thenReturn(Optional.of(mock(ProjectMember.class)));
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(projects, members, users);

        useCase.execute(8L, 1L);

        verify(members, never()).save(any());
        verify(projects, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void 멤버가_아니면_EDITOR로_등록한다() {
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        User owner = User.create("owner@example.com").withId(2L);
        Project project = new Project(owner, "밤하늘 위 멜로디", null, 701L);
        User joiner = User.create("joiner@example.com").withId(1L);
        when(members.findByProjectIdAndUserId(8L, 1L)).thenReturn(Optional.empty());
        when(projects.findByIdAndDeletedAtIsNull(8L)).thenReturn(Optional.of(project));
        when(users.findById(1L)).thenReturn(Optional.of(joiner));
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(projects, members, users);

        useCase.execute(8L, 1L);

        verify(members).save(argThatIsEditorMemberOf(project, joiner));
    }

    @Test
    void 프로젝트가_없거나_삭제됐으면_PROJECT_NOT_FOUND_예외가_발생한다() {
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(members.findByProjectIdAndUserId(8L, 1L)).thenReturn(Optional.empty());
        when(projects.findByIdAndDeletedAtIsNull(8L)).thenReturn(Optional.empty());
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(projects, members, users);

        assertThatThrownBy(() -> useCase.execute(8L, 1L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND);

        verify(members, never()).save(any());
    }

    @Test
    void 사용자가_없거나_비활성이면_PROJECT_ACCESS_DENIED_예외가_발생한다() {
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        User owner = User.create("owner@example.com").withId(2L);
        Project project = new Project(owner, "밤하늘 위 멜로디", null, 701L);
        when(members.findByProjectIdAndUserId(8L, 1L)).thenReturn(Optional.empty());
        when(projects.findByIdAndDeletedAtIsNull(8L)).thenReturn(Optional.of(project));
        when(users.findById(1L)).thenReturn(Optional.empty());
        ProjectMemberJoinUseCase useCase = new ProjectMemberJoinUseCase(projects, members, users);

        assertThatThrownBy(() -> useCase.execute(8L, 1L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);

        verify(members, never()).save(any());
    }

    private static ProjectMember argThatIsEditorMemberOf(Project project, User user) {
        return org.mockito.ArgumentMatchers.argThat(member ->
                member.getProject() == project
                        && member.getUser() == user
                        && member.getRole() == ProjectMemberRole.EDITOR);
    }
}
