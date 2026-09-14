package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.dto.request.ProjectDeleteRequest;
import glitched.adlips.application.project.usecase.ProjectDeleteUseCase;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectDeleteUseCaseTest {
    @Test
    void ownerSoftDeletesProject() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));

        new ProjectDeleteUseCase(projects, Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC))
                .execute(new ProjectDeleteRequest(10L, 1L));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DELETED);
        assertThat(project.getDeletedAt()).isNotNull();
        assertThat(project.isPublic()).isFalse();
    }
}
