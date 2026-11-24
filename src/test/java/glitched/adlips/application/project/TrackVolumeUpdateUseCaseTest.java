package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.TrackVolumeUpdateRequest;
import glitched.adlips.application.project.usecase.TrackVolumeUpdateUseCase;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackVolumeUpdateUseCaseTest {
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    private final User owner = User.create("owner@example.com").withId(1L);
    private final Project project = new Project(owner, "곡", null, 701L);

    @Test
    void updatesApprovedTrackVolumeAndInvalidatesRenderedMediaUnderTheProjectLock() {
        ProjectTrack track = new ProjectTrack(project, owner, "Guitar", "GUITAR", 0);
        track.approve();
        track.replaceRenderedMedia(601L);
        when(tracks.findProjectIdByTrackIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(7L));
        when(projects.findByIdAndDeletedAtIsNullForUpdate(7L)).thenReturn(Optional.of(project));
        when(tracks.findTrackByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(track));

        var response = new TrackVolumeUpdateUseCase(projects, tracks)
                .execute(new TrackVolumeUpdateRequest(101L, 1L, 75));

        assertThat(response.volume()).isEqualTo(75);
        assertThat(response.trackMediaFileId()).isNull();
        assertThat(response.projectVersion().display()).isEqualTo("v1.2");
        var order = inOrder(projects, tracks);
        order.verify(projects).findByIdAndDeletedAtIsNullForUpdate(7L);
        order.verify(tracks).findTrackByIdAndIsDeletedFalse(101L);
    }

    @Test
    void trackDeletedWhileWaitingForTheProjectLockIsNotWrittenBack() {
        when(tracks.findProjectIdByTrackIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(7L));
        when(projects.findByIdAndDeletedAtIsNullForUpdate(7L)).thenReturn(Optional.of(project));
        when(tracks.findTrackByIdAndIsDeletedFalse(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new TrackVolumeUpdateUseCase(projects, tracks)
                .execute(new TrackVolumeUpdateRequest(101L, 1L, 75)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.TRACK_NOT_FOUND);
        assertThat(project.getMinorVersion()).isEqualTo(1);
    }

    @Test
    void rejectsVolumeOutsideZeroToOneHundred() {
        assertThatThrownBy(() -> new TrackVolumeUpdateUseCase(projects, tracks)
                .execute(new TrackVolumeUpdateRequest(101L, 1L, 101)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.INVALID_TRACK_VOLUME);
    }
}
