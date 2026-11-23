package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.dto.request.ProjectCreateRequest;
import glitched.adlips.application.project.dto.response.ProjectCreateResponse;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectCreateUseCaseTest {
    @Test
    void createsProjectWithoutAlbumImage() {
        User owner = User.create("owner@example.com").withId(1L);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectCreateResponse response = new ProjectCreateUseCase(
                users, mock(MediaFileRepositoryPort.class), projects, members)
                .execute(new ProjectCreateRequest(1L, "새 곡", null, null));

        assertThat(response.albumImageFileId()).isZero();
        assertThat(response.albumImageUrl()).isNull();
    }

    @Test
    void createsProjectAtVersionOneOneAndOwnerMembership() {
        User owner = User.create("owner@example.com").withId(1L);
        MediaFile image = MediaFile.readyImage(1L, "https://cdn/album.png", "album/1", "album.png", "image/png", 10).withId(701L);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(media.findById(701L)).thenReturn(Optional.of(image));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectCreateUseCase useCase = new ProjectCreateUseCase(users, media, projects, members);

        ProjectCreateResponse response = useCase.execute(new ProjectCreateRequest(1L, "새 곡", "설명", 701L));

        assertThat(response.version().display()).isEqualTo("v1.1");
        assertThat(response.albumImageUrl()).isEqualTo("https://cdn/album.png");
        verify(members).save(any(ProjectMember.class));
    }

    @Test
    void rejectsAlbumImageOwnedByAnotherUser() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.create("owner@example.com").withId(1L)));
        when(media.findById(701L)).thenReturn(Optional.of(MediaFile.readyImage(
                2L, "https://cdn/album.png", "album/1", "album.png", "image/png", 10).withId(701L)));
        ProjectCreateUseCase useCase = new ProjectCreateUseCase(
                users, media, mock(ProjectRepositoryPort.class), mock(ProjectMemberRepositoryPort.class));

        assertThatThrownBy(() -> useCase.execute(new ProjectCreateRequest(1L, "새 곡", null, 701L)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode").isEqualTo(ProjectErrorCode.MEDIA_FILE_ACCESS_DENIED);
    }

    @Test
    void enforcesTitleAndDescriptionLengthLimits() {
        User owner = User.create("owner@example.com").withId(1L);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectCreateUseCase useCase = new ProjectCreateUseCase(
                users, mock(MediaFileRepositoryPort.class), projects, members);

        assertThatThrownBy(() -> useCase.execute(new ProjectCreateRequest(1L, "x".repeat(41), null, null)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode").isEqualTo(ProjectErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> useCase.execute(new ProjectCreateRequest(1L, "새 곡", "d".repeat(501), null)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode").isEqualTo(ProjectErrorCode.VALIDATION_ERROR);

        ProjectCreateResponse boundary = useCase.execute(
                new ProjectCreateRequest(1L, "x".repeat(40), "d".repeat(500), null));
        assertThat(boundary.title()).hasSize(40);
        assertThat(boundary.description()).hasSize(500);
    }
}
