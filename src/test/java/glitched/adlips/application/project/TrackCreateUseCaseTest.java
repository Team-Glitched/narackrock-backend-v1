package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.dto.response.TrackCreateResponse;
import glitched.adlips.application.project.usecase.TrackCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackCreateUseCaseTest {
    @Test
    void createsDraftTrackWithoutRenderedMedia() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        ProjectTrackJpaRepository tracks = mock(ProjectTrackJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(tracks.save(any(ProjectTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrackCreateResponse response = new TrackCreateUseCase(projects, members, tracks, users)
                .execute(new TrackCreateRequest(10L, 1L, "Guitar Track", "GUITAR", 0));

        assertThat(response.volume()).isEqualTo(100);
        assertThat(response.mediaFileId()).isNull();
        assertThat(response.approvalStatus().name()).isEqualTo("DRAFT");
    }
}
