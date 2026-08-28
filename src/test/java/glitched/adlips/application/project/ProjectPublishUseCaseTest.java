package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.ProjectPublishRequest;
import glitched.adlips.application.project.usecase.ProjectPublishUseCase;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectPublishUseCaseTest {
    @Test
    void queuesExportForApprovedTrackAndClip() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", null, 701L);
        ProjectTrack track = new ProjectTrack(project, owner, "트랙", "GUITAR", 0);
        track.approve();
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        ProjectExportRepositoryPort exports = mock(ProjectExportRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(10L, ApprovalStatus.APPROVED)).thenReturn(List.of(track));
        when(clips.countByTrackIdInAndApprovalStatusAndIsDeletedFalse(anyList(), eq(ApprovalStatus.APPROVED))).thenReturn(1L);
        when(exports.save(any(ProjectExport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new ProjectPublishUseCase(projects, members, tracks, clips, exports)
                .execute(new ProjectPublishRequest(10L, 1L));

        assertThat(response.exportStatus()).isEqualTo(ExportStatus.QUEUED);
        assertThat(response.mixedAudioFileId()).isNull();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }
}
