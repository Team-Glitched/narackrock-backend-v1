package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.application.project.usecase.ProjectCompleteUseCase;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectCompleteUseCaseTest {
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    private final PublishedShortPort publishedShorts = mock(PublishedShortPort.class);
    private final ProjectCompleteUseCase useCase = new ProjectCompleteUseCase(
            projects, members, publishedShorts, TransactionRunner.direct());
    private final User owner = User.create("owner@example.com").withId(1L);
    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project(owner, "곡", null, 701L);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
    }

    @Test
    void ownerCompletesPublishedProjectAndItsShorts() {
        project.startPublishing();
        project.publish(LocalDateTime.of(2026, 9, 16, 12, 0));

        var result = useCase.execute(10L, 1L);

        assertThat(result.status()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(result.isPublic()).isTrue();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(publishedShorts).completeProject(10L);
    }

    @Test
    void rejectsCompletionBeforePublicExport() {
        project.startPublishing();

        assertThatThrownBy(() -> useCase.execute(10L, 1L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_NOT_PUBLIC);
        verify(publishedShorts, never()).completeProject(10L);
    }

    @Test
    void onlyOwnerCanCompleteProject() {
        project.startPublishing();
        project.publish(LocalDateTime.of(2026, 9, 16, 12, 0));
        when(members.findByProjectIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(new ProjectMember(project,
                        User.create("editor@example.com").withId(2L), ProjectMemberRole.EDITOR)));

        assertThatThrownBy(() -> useCase.execute(10L, 2L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        verify(publishedShorts, never()).completeProject(10L);
    }
}
