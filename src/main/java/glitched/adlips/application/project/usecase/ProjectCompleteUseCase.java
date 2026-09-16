package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.response.ProjectCompleteResponse;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectStatus;

public class ProjectCompleteUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final PublishedShortPort publishedShorts;
    private final TransactionRunner transactionRunner;

    public ProjectCompleteUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            PublishedShortPort publishedShorts,
            TransactionRunner transactionRunner
    ) {
        this.projects = projects;
        this.members = members;
        this.publishedShorts = publishedShorts;
        this.transactionRunner = transactionRunner;
    }

    public ProjectCompleteResponse execute(Long projectId, Long userId) {
        return transactionRunner.required(() -> complete(projectId, userId));
    }

    private ProjectCompleteResponse complete(Long projectId, Long userId) {
        var project = projects.findByIdAndDeletedAtIsNullForUpdate(projectId)
                .orElseThrow(() -> new ProjectApplicationException(
                        ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않는 프로젝트입니다."));
        members.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> member.getRole() == ProjectMemberRole.OWNER)
                .orElseThrow(() -> new ProjectApplicationException(
                        ProjectErrorCode.PROJECT_ACCESS_DENIED, "곡을 완성할 권한이 없습니다."));
        if (!project.isPublic()) {
            throw new ProjectApplicationException(
                    ProjectErrorCode.PROJECT_NOT_PUBLIC, "공개된 곡만 완성할 수 있습니다.");
        }
        if (project.getStatus() != ProjectStatus.COMPLETED) {
            project.complete();
            publishedShorts.completeProject(projectId);
        }
        return new ProjectCompleteResponse(projectId, project.getStatus(), project.isPublic());
    }
}
