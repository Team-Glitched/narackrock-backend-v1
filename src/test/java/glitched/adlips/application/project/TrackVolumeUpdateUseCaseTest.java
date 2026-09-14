package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.TrackVolumeUpdateRequest;
import glitched.adlips.application.project.usecase.TrackVolumeUpdateUseCase;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackVolumeUpdateUseCaseTest {
    @Test
    void updatesApprovedTrackVolumeAndInvalidatesRenderedMedia() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "Guitar", "GUITAR", 0);
        track.approve();
        track.replaceRenderedMedia(601L);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        when(tracks.findTrackByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(track));

        var response = new TrackVolumeUpdateUseCase(tracks)
                .execute(new TrackVolumeUpdateRequest(101L, 1L, 75));

        assertThat(response.volume()).isEqualTo(75);
        assertThat(response.trackMediaFileId()).isNull();
        assertThat(response.projectVersion().display()).isEqualTo("v1.2");
    }

    @Test
    void rejectsVolumeOutsideZeroToOneHundred() {
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);

        assertThatThrownBy(() -> new TrackVolumeUpdateUseCase(tracks)
                .execute(new TrackVolumeUpdateRequest(101L, 1L, 101)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.INVALID_TRACK_VOLUME);
    }
}
