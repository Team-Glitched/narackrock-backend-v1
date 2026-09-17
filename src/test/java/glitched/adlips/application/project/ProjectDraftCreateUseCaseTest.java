package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.usecase.ProjectDraftCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectDraftCreateUseCaseTest {
    @Test
    void createsEditableDraftWithoutTitleOrImage() {
        User owner = User.create("owner@example.com").withId(1L);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new ProjectDraftCreateUseCase(users, projects, members).execute(1L);

        assertThat(result.albumImageFileId()).isZero();
        assertThat(result.title()).isEqualTo("제목 없는 곡");
        verify(members).save(any(ProjectMember.class));
    }
}
