package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.dto.request.ProjectExportGetRequest;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.usecase.ProjectExportGetUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectExport;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectExportGetUseCaseTest {

    private final ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
    private final ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
    private final ProjectExportRepositoryPort exports = mock(ProjectExportRepositoryPort.class);
    private final MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
    private final ProjectExportGetUseCase useCase =
            new ProjectExportGetUseCase(projects, members, exports, mediaFiles);

    private Project project;
    private ProjectExport export;

    @BeforeEach
    void setUp() {
        User owner = User.create("owner@example.com").withId(1L);
        project = new Project(owner, "곡", null, 701L);
        ReflectionTestUtils.setField(project, "id", 10L);
        export = new ProjectExport(project, owner);
        ReflectionTestUtils.setField(export, "id", 20L);

        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(exports.findExportByIdAndProjectId(20L, 10L)).thenReturn(Optional.of(export));
    }

    @Test
    void returnsQueuedExportStatusForProjectMember() {
        var response = useCase.execute(new ProjectExportGetRequest(10L, 20L, 1L));

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.exportId()).isEqualTo(20L);
        assertThat(response.projectVersion().display()).isEqualTo("v1.1");
        assertThat(response.status()).isEqualTo(glitched.adlips.domain.project.ExportStatus.QUEUED);
        assertThat(response.mixedAudioFileId()).isNull();
        assertThat(response.albumImageFileId()).isEqualTo(701L);
    }

    @Test
    void returnsGeneratedMediaUrlsForCompletedExport() {
        export.startProcessing();
        export.complete(801L, 802L, 32_000, LocalDateTime.of(2026, 9, 14, 10, 0));
        when(mediaFiles.findById(801L)).thenReturn(Optional.of(media(801L, "/files/audio/mix.wav")));
        when(mediaFiles.findById(701L)).thenReturn(Optional.of(media(701L, "/files/images/album.png")));

        var response = useCase.execute(new ProjectExportGetRequest(10L, 20L, 1L));

        assertThat(response.mixedAudioFileId()).isEqualTo(801L);
        assertThat(response.mixedAudioUrl()).isEqualTo("/files/audio/mix.wav");
        assertThat(response.layerArchiveFileId()).isEqualTo(802L);
        assertThat(response.albumImageUrl()).isEqualTo("/files/images/album.png");
        assertThat(response.durationMs()).isEqualTo(32_000);
        assertThat(response.completedAt()).isEqualTo(LocalDateTime.of(2026, 9, 14, 10, 0));
    }

    @Test
    void rejectsNonMember() {
        when(members.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ProjectExportGetRequest(10L, 20L, 2L)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void rejectsExportOutsideProjectAsNotFound() {
        when(exports.findExportByIdAndProjectId(20L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ProjectExportGetRequest(10L, 20L, 1L)))
                .isInstanceOf(ProjectApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.EXPORT_NOT_FOUND);
    }

    private MediaFile media(Long id, String url) {
        return MediaFile.restore(id, 1L, url, "test/" + id, "file", MediaFileType.AUDIO,
                "audio/wav", 100L, MediaFileStatus.READY);
    }
}
