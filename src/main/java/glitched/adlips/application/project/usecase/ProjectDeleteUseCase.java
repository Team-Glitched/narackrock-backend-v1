package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectDeleteRequest;
import glitched.adlips.application.project.dto.response.ProjectDeleteResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectDeleteUseCase {
    private final ProjectJpaRepository projects;
    private final Clock clock;
    public ProjectDeleteUseCase(ProjectJpaRepository projects, Clock clock) {
        this.projects = projects; this.clock = clock;
    }

    @Transactional
    public ProjectDeleteResponse execute(ProjectDeleteRequest request) {
        var project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND, "존재하지 않거나 이미 삭제된 프로젝트입니다."));
        if (!request.userId().equals(project.getOwner().getId())) {
            throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED, "곡을 삭제할 권한이 없습니다.");
        }
        project.delete(LocalDateTime.now(clock));
        return new ProjectDeleteResponse(project.getId());
    }
    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
