package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.dto.request.TrackDeleteRequest;
import glitched.adlips.application.project.port.out.ProjectContributionItemRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.usecase.TrackDeleteUseCase;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackDeleteUseCaseTest {
    @Test
    void deletesSampleTrackOnlyAfterTakingTheProjectLock() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack sample = new ProjectTrack(project, owner, "Sample", "drum-kit", 0, 55L);
        sample.approve();
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        when(tracks.findProjectIdByTrackIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(7L));
        when(projects.findByIdAndDeletedAtIsNullForUpdate(7L)).thenReturn(Optional.of(project));
        when(tracks.findTrackByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(sample));

        var response = new TrackDeleteUseCase(projects, tracks, mock(ProjectContributionItemRepositoryPort.class))
                .execute(new TrackDeleteRequest(101L, 1L));

        assertThat(sample.isDeleted()).isTrue();
        assertThat(response.projectVersion().display()).isEqualTo("v1.2");
        var order = inOrder(projects, tracks);
        order.verify(projects).findByIdAndDeletedAtIsNullForUpdate(7L);
        order.verify(tracks).findTrackByIdAndIsDeletedFalse(101L);
    }
}
