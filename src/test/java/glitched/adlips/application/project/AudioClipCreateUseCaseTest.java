package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.dto.request.AudioClipCreateRequest;
import glitched.adlips.application.project.usecase.AudioClipCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AudioClipCreateUseCaseTest {
    @Test
    void createsDraftAudioClipAndInvalidatesRenderedTrack() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "Guitar", "GUITAR", 0);
        track.replaceRenderedMedia(900L);
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        ProjectTrackJpaRepository tracks = mock(ProjectTrackJpaRepository.class);
        ProjectClipJpaRepository clips = mock(ProjectClipJpaRepository.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(
                Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(track));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(mediaFiles.findById(501L)).thenReturn(Optional.of(MediaFile.restore(
                501L, 1L, "http://localhost/files/guitar.wav", "media/1/guitar.wav", "guitar.wav",
                MediaFileType.AUDIO, "audio/wav", 100L, MediaFileStatus.READY)));
        when(clips.save(any(ProjectClip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new AudioClipCreateUseCase(projects, members, tracks, clips, users, mediaFiles)
                .execute(new AudioClipCreateRequest(
                        10L, 21L, 1L, 501L, ClipSourceType.RECORDING, 0, 1920, 0));

        assertThat(response.clipType().name()).isEqualTo("AUDIO");
        assertThat(response.durationMs()).isEqualTo(2000);
        assertThat(response.mediaUrl()).contains("guitar.wav");
        assertThat(track.getMediaFileId()).isNull();
    }
}
