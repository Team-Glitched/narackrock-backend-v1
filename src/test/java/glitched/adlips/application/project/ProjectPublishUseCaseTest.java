package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import glitched.adlips.adapter.out.persistence.project.*;
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
        ProjectJpaRepository projects = mock(ProjectJpaRepository.class);
        ProjectMemberJpaRepository members = mock(ProjectMemberJpaRepository.class);
        ProjectTrackJpaRepository tracks = mock(ProjectTrackJpaRepository.class);
        ProjectClipJpaRepository clips = mock(ProjectClipJpaRepository.class);
        ProjectExportJpaRepository exports = mock(ProjectExportJpaRepository.class);
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
