package glitched.adlips.adapter.out.persistence.project;

import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionItemRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.project.ProjectExport;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectTrack;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectPersistenceAdapter implements
        ProjectRepositoryPort,
        ProjectMemberRepositoryPort,
        ProjectTrackRepositoryPort,
        ProjectClipRepositoryPort,
        ProjectContributionRepositoryPort,
        ProjectContributionItemRepositoryPort,
        ProjectExportRepositoryPort {

    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final ProjectTrackJpaRepository tracks;
    private final ProjectClipJpaRepository clips;
    private final ProjectContributionJpaRepository contributions;
    private final ProjectContributionItemJpaRepository contributionItems;
    private final ProjectExportJpaRepository exports;

    public ProjectPersistenceAdapter(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            ProjectContributionJpaRepository contributions,
            ProjectContributionItemJpaRepository contributionItems,
            ProjectExportJpaRepository exports
    ) {
        this.projects = projects;
        this.members = members;
        this.tracks = tracks;
        this.clips = clips;
        this.contributions = contributions;
        this.contributionItems = contributionItems;
        this.exports = exports;
    }

    @Override
    public Optional<Project> findByIdAndDeletedAtIsNull(Long id) {
        return projects.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Project> findByIdAndDeletedAtIsNullForUpdate(Long id) {
        return projects.findByIdAndDeletedAtIsNullForUpdate(id);
    }

    @Override
    public Project save(Project project) {
        return projects.save(project);
    }

    @Override
    public Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId) {
        return members.findByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public ProjectMember save(ProjectMember member) {
        return members.save(member);
    }

    @Override
    public Optional<ProjectTrack> findTrackByIdAndIsDeletedFalse(Long id) {
        return tracks.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public List<ProjectTrack> findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(Long projectId) {
        return tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(projectId);
    }

    @Override
    public List<ProjectTrack> findByProjectIdAndApprovalStatusAndIsDeletedFalse(
            Long projectId, ApprovalStatus status) {
        return tracks.findByProjectIdAndApprovalStatusAndIsDeletedFalse(projectId, status);
    }

    @Override
    public ProjectTrack save(ProjectTrack track) {
        return tracks.save(track);
    }

    @Override
    public Optional<ProjectClip> findClipByIdAndIsDeletedFalse(Long id) {
        return clips.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public List<ProjectClip> findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(Long trackId) {
        return clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(trackId);
    }

    @Override
    public long countByTrackIdInAndApprovalStatusAndIsDeletedFalse(
            List<Long> trackIds, ApprovalStatus status) {
        return clips.countByTrackIdInAndApprovalStatusAndIsDeletedFalse(trackIds, status);
    }

    @Override
    public ProjectClip save(ProjectClip clip) {
        return clips.save(clip);
    }

    @Override
    public Optional<ProjectContribution> findByIdAndProjectId(Long id, Long projectId) {
        return contributions.findByIdAndProjectId(id, projectId);
    }

    @Override
    public List<ProjectContribution> findByProjectIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, int pageSize) {
        return contributions.findByProjectIdAndApprovalStatusInOrderByIdDesc(projectId, statuses, pageSize);
    }

    @Override
    public List<ProjectContribution> findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Collection<ContributionApprovalStatus> statuses, Long cursor, int pageSize) {
        return contributions.findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
                projectId, statuses, cursor, pageSize);
    }

    @Override
    public List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses, int pageSize) {
        return contributions.findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
                projectId, userId, statuses, pageSize);
    }

    @Override
    public List<ProjectContribution> findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
            Long projectId, Long userId, Collection<ContributionApprovalStatus> statuses,
            Long cursor, int pageSize) {
        return contributions.findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
                projectId, userId, statuses, cursor, pageSize);
    }

    @Override
    public ProjectContribution save(ProjectContribution contribution) {
        return contributions.save(contribution);
    }

    @Override
    public boolean existsByTargetTypeAndTargetIdAndContributionApprovalStatus(
            ContributionTargetType targetType, Long targetId, ContributionApprovalStatus status) {
        return contributionItems.existsByTargetTypeAndTargetIdAndContributionApprovalStatus(
                targetType, targetId, status);
    }

    @Override
    public void saveAll(List<ProjectContributionItem> items) {
        contributionItems.saveAll(items);
    }

    @Override
    public Optional<ProjectExport> findExportByIdAndProjectId(Long id, Long projectId) {
        return exports.findExportByIdAndProjectId(id, projectId);
    }

    @Override
    public ProjectExport save(ProjectExport projectExport) {
        return exports.save(projectExport);
    }
}
