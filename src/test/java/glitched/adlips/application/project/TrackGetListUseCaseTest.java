package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.TrackGetListRequest;
import glitched.adlips.application.project.usecase.TrackGetListUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackGetListUseCaseTest {
    @Test
    void returnsTimelineAndOwnerPermissions() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", "설명", 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());

        var response = new TrackGetListUseCase(projects, members, tracks, clips, mock(MediaFileRepositoryPort.class))
                .execute(new TrackGetListRequest(10L, 1L));

        assertThat(response.project().maxTick()).isEqualTo(57_600L);
        assertThat(response.viewerPermission().canExport()).isTrue();
        assertThat(response.tracks()).isEmpty();
    }
}
