package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.application.project.usecase.ProjectMetadataUpdateUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectMetadataUpdateUseCaseTest {
    @Test
    void addsTitleWithoutAlbumImageToDraft() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "제목 없는 곡", null, 0L);
        var projects = mock(ProjectRepositoryPort.class);
        var members = mock(ProjectMemberRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));

        new ProjectMetadataUpdateUseCase(projects, members, mock(MediaFileRepositoryPort.class), mock(PublishedShortPort.class))
                .execute(10L, 1L, "  새 곡  ", null);

        assertThat(project.getTitle()).isEqualTo("새 곡");
        assertThat(project.getAlbumImageFileId()).isZero();
    }

    @Test
    void addsTitleAndReadyOwnedImageToDraft() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "제목 없는 곡", null, 0L);
        var projects = mock(ProjectRepositoryPort.class);
        var members = mock(ProjectMemberRepositoryPort.class);
        var media = mock(MediaFileRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(media.findById(701L)).thenReturn(Optional.of(MediaFile.readyImage(1L,
                "https://cdn/album.png", "album/1", "album.png", "image/png", 10).withId(701L)));

        new ProjectMetadataUpdateUseCase(projects, members, media, mock(PublishedShortPort.class)).execute(10L, 1L, "  새 곡  ", 701L);

        assertThat(project.getTitle()).isEqualTo("새 곡");
        assertThat(project.getAlbumImageFileId()).isEqualTo(701L);
    }

    @Test
    void rejectsNonOwner() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "제목 없는 곡", null, 0L);
        var projects = mock(ProjectRepositoryPort.class);
        var members = mock(ProjectMemberRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ProjectMetadataUpdateUseCase(projects, members,
                mock(MediaFileRepositoryPort.class), mock(PublishedShortPort.class)).execute(10L, 2L, "새 곡", 701L))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode").isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }
    @Test
    void originalOwnerCanEditPublishedAndCompletedMetadata() {
        for (boolean completed : new boolean[]{false, true}) {
            User owner = User.create("owner@example.com").withId(1L);
            Project project = new Project(owner, "원래 제목", null, 0L);
            project.startPublishing();
            project.publish(java.time.LocalDateTime.now());
            if (completed) project.complete();
            var projects = mock(ProjectRepositoryPort.class);
            var members = mock(ProjectMemberRepositoryPort.class);
            when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
            when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));

            new ProjectMetadataUpdateUseCase(projects, members, mock(MediaFileRepositoryPort.class), mock(PublishedShortPort.class))
                    .execute(10L, 1L, "수정한 제목", null);

            assertThat(project.getTitle()).isEqualTo("수정한 제목");
            assertThat(project.isPublic()).isTrue();
            assertThat(project.getAlbumImageFileId()).isZero();
        }
    }

    @Test
    void rejectsEditorAndInvalidImageWithoutChangingPublishedMetadata() {
        User owner = User.create("owner@example.com").withId(1L);
        User editor = User.create("editor@example.com").withId(2L);
        Project project = new Project(owner, "원래 제목", null, 0L);
        project.startPublishing();
        project.publish(java.time.LocalDateTime.now());
        var projects = mock(ProjectRepositoryPort.class);
        var members = mock(ProjectMemberRepositoryPort.class);
        var media = mock(MediaFileRepositoryPort.class);
        var shorts = mock(PublishedShortPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.of(new ProjectMember(project, editor, ProjectMemberRole.EDITOR)));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(media.findById(701L)).thenReturn(Optional.of(MediaFile.readyImage(2L,
                "https://cdn/album.png", "album/2", "album.png", "image/png", 10).withId(701L)));
        var useCase = new ProjectMetadataUpdateUseCase(projects, members, media, shorts);
        assertThatThrownBy(() -> useCase.execute(10L, 2L, "침입", null)).isInstanceOf(ProjectApplicationException.class);
        assertThatThrownBy(() -> useCase.execute(10L, 1L, "수정", 701L)).isInstanceOf(ProjectApplicationException.class);
        assertThatThrownBy(() -> useCase.execute(10L, 1L, " ", null)).isInstanceOf(ProjectApplicationException.class);
        assertThatThrownBy(() -> useCase.execute(10L, 1L, "x".repeat(41), null)).isInstanceOf(ProjectApplicationException.class);
        assertThat(project.getTitle()).isEqualTo("원래 제목");
        org.mockito.Mockito.verifyNoInteractions(shorts);

        useCase.execute(10L, 1L, "x".repeat(40), null);
        assertThat(project.getTitle()).isEqualTo("x".repeat(40));
    }

}
