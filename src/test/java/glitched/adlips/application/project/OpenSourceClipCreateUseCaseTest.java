package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.opensource.OpenSourceMediaPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.dto.request.OpenSourceClipCreateRequest;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.usecase.OpenSourceClipCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.ClipSourceType;
import glitched.adlips.domain.project.ClipType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;
import glitched.adlips.domain.project.ProjectTrack;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpenSourceClipCreateUseCaseTest {
    private final User caller = User.create("caller@example.com").withId(1L);
    private final User other = User.create("other@example.com").withId(2L);
    private final Long sourceOwnerId = 3L;
    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    private final ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
    private final ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
    private final OpenSourceMediaPort openSources = mock(OpenSourceMediaPort.class);
    private final OpenSourceClipCreateUseCase useCase = new OpenSourceClipCreateUseCase(
            projects, members, tracks, clips, users, mediaFiles, openSources, TransactionRunner.direct());
    private Project project;
    private ProjectTrack track;

    @BeforeEach
    void setUp() {
        project = new Project(caller, "곡", null, 701L);
        track = new ProjectTrack(project, caller, "Guitar", "GUITAR", 0);
        track.replaceRenderedMedia(900L);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, caller, ProjectMemberRole.EDITOR)));
        when(tracks.findTrackByIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(track));
        when(users.findById(1L)).thenReturn(Optional.of(caller));
        when(openSources.findAvailable(55L)).thenReturn(Optional.of(new OpenSourceMediaPort.Source(sourceOwnerId, 777L)));
        when(mediaFiles.findById(777L)).thenReturn(Optional.of(media(sourceOwnerId, MediaFileStatus.READY)));
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(21L)).thenReturn(List.of());
        when(clips.save(any(ProjectClip.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void importsAnotherUsersPublicSourceAsAnImportedAudioClipOnTheCallersLayer() {
        var response = useCase.execute(request(55L, 480, 1920));

        assertThat(response.clipType()).isEqualTo(ClipType.AUDIO);
        assertThat(response.sourceType()).isEqualTo(ClipSourceType.IMPORTED);
        assertThat(response.mediaFileId()).isEqualTo(777L);
        assertThat(response.mediaUrl()).isEqualTo("http://localhost/files/riff.wav");
        assertThat(response.startTick()).isEqualTo(480);
        assertThat(response.durationTick()).isEqualTo(1920);
        assertThat(response.durationMs()).isEqualTo(project.tickToMilliseconds(1920));
        assertThat(response.clipOffsetMs()).isZero();
        assertThat(track.getMediaFileId()).isNull();
        verify(clips).save(any(ProjectClip.class));
    }

    @Test
    void rejectsAnotherMembersLayer() {
        track = new ProjectTrack(project, other, "Piano", "PIANO", 1);
        when(tracks.findTrackByIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(track));

        assertError(request(55L, 0, 480), ProjectErrorCode.TRACK_EDIT_DENIED);
    }

    @Test
    void rejectsCompletedProjects() {
        ReflectionTestUtils.setField(project, "status", ProjectStatus.COMPLETED);

        assertError(request(55L, 0, 480), ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void rejectsViewers() {
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, caller, ProjectMemberRole.VIEWER)));

        assertError(request(55L, 0, 480), ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void rejectsPrivateDeletedOrMissingSources() {
        // findAvailable filters private, deleted, owner-deleted and incomplete sources.
        when(openSources.findAvailable(56L)).thenReturn(Optional.empty());

        assertError(request(56L, 0, 480), ProjectErrorCode.OPEN_SOURCE_NOT_FOUND);
    }

    @Test
    void rejectsSourceMediaThatIsNotTheOwnersReadyAudio() {
        when(mediaFiles.findById(777L)).thenReturn(Optional.of(media(1L, MediaFileStatus.READY)));
        assertError(request(55L, 0, 480), ProjectErrorCode.MEDIA_FILE_NOT_READY);

        when(mediaFiles.findById(777L)).thenReturn(Optional.of(media(sourceOwnerId, MediaFileStatus.PROCESSING)));
        assertError(request(55L, 0, 480), ProjectErrorCode.MEDIA_FILE_NOT_READY);

        when(mediaFiles.findById(777L)).thenReturn(Optional.empty());
        assertError(request(55L, 0, 480), ProjectErrorCode.MEDIA_FILE_NOT_READY);
    }

    @Test
    void rejectsOutOfRangeAndInvalidInput() {
        assertError(request(55L, -1, 480), ProjectErrorCode.INVALID_CLIP_RANGE);
        assertError(request(55L, 0, 0), ProjectErrorCode.INVALID_CLIP_RANGE);
        assertError(request(55L, (int) project.getMaxTick() - 100, 480), ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED);
        assertError(request(null, 0, 480), ProjectErrorCode.VALIDATION_ERROR);
        assertError(request(0L, 0, 480), ProjectErrorCode.VALIDATION_ERROR);
    }

    private void assertError(OpenSourceClipCreateRequest request, ProjectErrorCode code) {
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode").isEqualTo(code);
        verify(clips, never()).save(any(ProjectClip.class));
    }

    private static OpenSourceClipCreateRequest request(Long openSourceId, int startTick, int durationTick) {
        return new OpenSourceClipCreateRequest(10L, 21L, 1L, openSourceId, startTick, durationTick);
    }

    private static MediaFile media(Long ownerId, MediaFileStatus status) {
        return MediaFile.restore(777L, ownerId, "http://localhost/files/riff.wav", "media/3/riff.wav", "riff.wav",
                MediaFileType.AUDIO, "audio/wav", 100L, status);
    }
}
