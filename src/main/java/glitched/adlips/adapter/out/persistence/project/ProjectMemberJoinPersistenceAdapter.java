package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.application.project.port.out.ProjectMemberJoinPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectMemberJoinPersistenceAdapter implements ProjectMemberJoinPort {

    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;

    public ProjectMemberJoinPersistenceAdapter(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members
    ) {
        this.projects = projects;
        this.members = members;
    }

    @Override
    public Optional<Project> findActiveProjectForUpdate(Long projectId) {
        return projects.findByIdAndDeletedAtIsNullForUpdate(projectId);
    }

    @Override
    public boolean existsByProjectIdAndUserId(Long projectId, Long userId) {
        return members.existsByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public void saveEditor(Project project, User user) {
        members.save(new ProjectMember(project, user, ProjectMemberRole.EDITOR));
    }
}
