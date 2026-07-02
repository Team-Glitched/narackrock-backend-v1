package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionItemJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.dto.request.ProjectVersionRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionCreateResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.ApprovalStatus;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectContributionItem;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.project.ProjectTrack;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectContributionCreateUseCase {
    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final ProjectContributionJpaRepository contributions;
    private final ProjectContributionItemJpaRepository contributionItems;
    private final ProjectTrackJpaRepository tracks;
    private final ProjectClipJpaRepository clips;
    private final UserRepositoryPort users;

    public ProjectContributionCreateUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectContributionJpaRepository contributions,
            ProjectContributionItemJpaRepository contributionItems,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users) {
        this.projects = projects;
        this.members = members;
        this.contributions = contributions;
        this.contributionItems = contributionItems;
        this.tracks = tracks;
        this.clips = clips;
        this.users = users;
    }

    @Transactional
    public ProjectContributionCreateResponse execute(ProjectContributionCreateRequest request) {
        validateRequest(request);
        Project project = projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND,
                        "존재하지 않거나 삭제된 프로젝트입니다."));
        members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .filter(member -> member.getRole() != ProjectMemberRole.VIEWER)
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED,
                        "해당 프로젝트에 기여할 권한이 없습니다."));
        var user = users.findById(request.userId()).filter(it -> it.isActive())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED,
                        "해당 프로젝트에 기여할 권한이 없습니다."));

        validateTargets(project, user.getId(), request.items());
        int baseMajor = project.getMajorVersion();
        int baseMinor = project.getMinorVersion();
        if (requiresBaseVersion(request.items())) {
            ProjectVersionRequest base = request.baseProjectVersion();
            if (base == null || base.major() < 1 || base.minor() < 1) {
                throw error(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM,
                        "UPDATE 또는 DELETE 기여에는 기준 프로젝트 버전이 필요합니다.");
            }
            baseMajor = base.major();
            baseMinor = base.minor();
        }

        ProjectContribution contribution = new ProjectContribution(
                project, user, normalize(request.description()), baseMajor, baseMinor);
        List<ProjectContributionItem> savedItems = request.items().stream()
                .map(item -> new ProjectContributionItem(contribution, item.changeType(),
                        item.targetType(), item.targetId()))
                .toList();
        ProjectContribution saved = contributions.save(contribution);
        contributionItems.saveAll(savedItems);
        markAddTargetsPending(request.items());

        return new ProjectContributionCreateResponse(
                request.projectId(), saved.getId(),
                new ProjectVersionResponse(saved.getBaseMajorVersion(), saved.getBaseMinorVersion(),
                        "v" + saved.getBaseMajorVersion() + "." + saved.getBaseMinorVersion()),
                saved.getApprovalStatus(), saved.getDescription(),
                savedItems.stream().map(item -> new ProjectContributionCreateResponse.Item(
                        item.getChangeType(), item.getTargetType(), item.getTargetId(), item.getItemStatus()))
                        .toList(),
                saved.getCreatedAt());
    }

    private void validateRequest(ProjectContributionCreateRequest request) {
        if (request == null || request.projectId() == null || request.userId() == null
                || request.items() == null || request.items().isEmpty()) {
            throw error(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM, "기여 항목 정보가 올바르지 않습니다.");
        }
        Set<String> uniqueTargets = new HashSet<>();
        for (var item : request.items()) {
            if (item == null || item.changeType() == null || item.targetType() == null
                    || item.targetId() == null || item.targetId() < 1
                    || !uniqueTargets.add(item.targetType() + ":" + item.targetId())) {
                throw error(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM, "기여 항목 정보가 올바르지 않습니다.");
            }
        }
    }

    private void validateTargets(Project project, Long userId,
                                 List<ProjectContributionCreateRequest.Item> items) {
        Set<Long> submittedClipIds = items.stream()
                .filter(item -> item.changeType() == ContributionChangeType.ADD
                        && item.targetType() == ContributionTargetType.CLIP)
                .map(ProjectContributionCreateRequest.Item::targetId)
                .collect(java.util.stream.Collectors.toSet());

        for (var item : items) {
            if (item.targetType() == ContributionTargetType.TRACK) {
                ProjectTrack track = tracks.findByIdAndIsDeletedFalse(item.targetId())
                        .orElseThrow(() -> invalidItem());
                validateTrack(project, userId, item, track);
                if (item.changeType() == ContributionChangeType.ADD) {
                    boolean omittedClip = clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(item.targetId())
                            .stream().anyMatch(clip -> !submittedClipIds.contains(clip.getId()));
                    if (omittedClip) {
                        throw error(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM,
                                "트랙의 모든 클립을 함께 제출해야 합니다.");
                    }
                }
            } else {
                ProjectClip clip = clips.findByIdAndIsDeletedFalse(item.targetId())
                        .orElseThrow(() -> invalidItem());
                validateClip(project, userId, item, clip);
            }
        }
    }

    private void validateTrack(Project project, Long userId,
                               ProjectContributionCreateRequest.Item item, ProjectTrack track) {
        if (!track.belongsTo(project)) {
            throw invalidItem();
        }
        validateTargetState(userId, item, track.getOwner().getId(), track.getApprovalStatus());
    }

    private void validateClip(Project project, Long userId,
                              ProjectContributionCreateRequest.Item item, ProjectClip clip) {
        if (clip.getProject() != project
                && (clip.getProject().getId() == null || !clip.getProject().getId().equals(project.getId()))) {
            throw invalidItem();
        }
        validateTargetState(userId, item, clip.getOwner().getId(), clip.getApprovalStatus());
    }

    private void validateTargetState(Long userId, ProjectContributionCreateRequest.Item item,
                                     Long targetOwnerId, ApprovalStatus status) {
        if (item.changeType() == ContributionChangeType.ADD) {
            if (!userId.equals(targetOwnerId) || status != ApprovalStatus.DRAFT) {
                throw invalidItem();
            }
        } else if (status != ApprovalStatus.APPROVED) {
            throw invalidItem();
        }
    }

    private void markAddTargetsPending(List<ProjectContributionCreateRequest.Item> items) {
        for (var item : items) {
            if (item.changeType() != ContributionChangeType.ADD) {
                continue;
            }
            if (item.targetType() == ContributionTargetType.TRACK) {
                tracks.findByIdAndIsDeletedFalse(item.targetId()).orElseThrow(this::invalidItem).submitForReview();
            } else {
                clips.findByIdAndIsDeletedFalse(item.targetId()).orElseThrow(this::invalidItem).submitForReview();
            }
        }
    }

    private boolean requiresBaseVersion(List<ProjectContributionCreateRequest.Item> items) {
        return items.stream().anyMatch(item -> item.changeType() != ContributionChangeType.ADD);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProjectApplicationException invalidItem() {
        return error(ProjectErrorCode.INVALID_CONTRIBUTION_ITEM, "기여 항목 정보가 올바르지 않습니다.");
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
