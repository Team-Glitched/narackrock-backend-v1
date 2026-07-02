package glitched.adlips.application.project.usecase;

import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.request.ProjectContributionGetListRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionGetListResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.project.ContributionApprovalStatus;
import glitched.adlips.domain.project.ProjectContribution;
import glitched.adlips.domain.project.ProjectMemberRole;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectContributionGetListUseCase {
    private final ProjectJpaRepository projects;
    private final ProjectMemberJpaRepository members;
    private final ProjectContributionJpaRepository contributions;
    private final ProfileRepositoryPort profiles;
    private final MediaFileRepositoryPort mediaFiles;

    public ProjectContributionGetListUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectContributionJpaRepository contributions,
            ProfileRepositoryPort profiles,
            MediaFileRepositoryPort mediaFiles) {
        this.projects = projects;
        this.members = members;
        this.contributions = contributions;
        this.profiles = profiles;
        this.mediaFiles = mediaFiles;
    }

    @Transactional(readOnly = true)
    public ProjectContributionGetListResponse execute(ProjectContributionGetListRequest request) {
        validateRequest(request);
        projects.findByIdAndDeletedAtIsNull(request.projectId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND,
                        "존재하지 않거나 삭제된 프로젝트입니다."));
        var member = members.findByProjectIdAndUserId(request.projectId(), request.userId())
                .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED,
                        "해당 프로젝트의 기여 요청을 조회할 권한이 없습니다."));

        List<ContributionApprovalStatus> statuses = parseStatuses(request.status());
        boolean canReviewAll = member.getRole() == ProjectMemberRole.OWNER
                || member.getRole() == ProjectMemberRole.EDITOR;
        PageRequest pageable = PageRequest.of(0, request.size() + 1);
        List<ProjectContribution> fetched = canReviewAll
                ? findProjectContributions(request, statuses, pageable)
                : findOwnContributions(request, statuses, pageable);

        boolean hasMore = fetched.size() > request.size();
        List<ProjectContributionGetListResponse.Contribution> items = fetched.stream()
                .limit(request.size())
                .map(contribution -> toResponse(request.projectId(), contribution))
                .toList();
        Long nextCursor = items.isEmpty() ? null : items.getLast().contributionId();
        return new ProjectContributionGetListResponse(
                items, new ProjectContributionGetListResponse.Page(nextCursor, hasMore, request.size()));
    }

    private List<ProjectContribution> findProjectContributions(
            ProjectContributionGetListRequest request,
            Collection<ContributionApprovalStatus> statuses,
            PageRequest pageable) {
        if (request.cursor() == null) {
            return contributions.findByProjectIdAndApprovalStatusInOrderByIdDesc(
                    request.projectId(), statuses, pageable);
        }
        return contributions.findByProjectIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
                request.projectId(), statuses, request.cursor(), pageable);
    }

    private List<ProjectContribution> findOwnContributions(
            ProjectContributionGetListRequest request,
            Collection<ContributionApprovalStatus> statuses,
            PageRequest pageable) {
        if (request.cursor() == null) {
            return contributions.findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(
                    request.projectId(), request.userId(), statuses, pageable);
        }
        return contributions.findByProjectIdAndUserIdAndApprovalStatusInAndIdLessThanOrderByIdDesc(
                request.projectId(), request.userId(), statuses, request.cursor(), pageable);
    }

    private ProjectContributionGetListResponse.Contribution toResponse(
            Long projectId, ProjectContribution contribution) {
        var profile = profiles.findByUserId(contribution.getUser().getId());
        String nickname = profile.map(it -> it.getNickname()).orElse(null);
        String profileImageUrl = profile.map(it -> it.getProfileImageFileId())
                .flatMap(mediaFiles::findById)
                .map(it -> it.getFileUrl())
                .orElse(null);
        return new ProjectContributionGetListResponse.Contribution(
                contribution.getId(), projectId, contribution.getUser().getId(), nickname, profileImageUrl,
                contribution.getDescription(),
                new ProjectVersionResponse(contribution.getBaseMajorVersion(), contribution.getBaseMinorVersion(),
                        "v" + contribution.getBaseMajorVersion() + "." + contribution.getBaseMinorVersion()),
                contribution.getApprovalStatus(),
                contribution.getReviewer() == null ? null : contribution.getReviewer().getId(),
                contribution.getReviewedAt(), contribution.getReviewComment(), contribution.getCreatedAt(),
                contribution.getItems().stream().map(item -> new ProjectContributionGetListResponse.Item(
                        item.getChangeType(), item.getTargetType(), item.getTargetId(), item.getItemStatus()))
                        .toList());
    }

    private List<ContributionApprovalStatus> parseStatuses(String value) {
        if (value == null || value.isBlank()) {
            return List.of(ContributionApprovalStatus.PENDING, ContributionApprovalStatus.APPROVED);
        }
        try {
            List<ContributionApprovalStatus> statuses = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(it -> !it.isEmpty())
                    .map(it -> ContributionApprovalStatus.valueOf(it.toUpperCase(Locale.ROOT)))
                    .distinct()
                    .toList();
            if (statuses.isEmpty() || statuses.contains(ContributionApprovalStatus.REJECTED)) {
                throw new IllegalArgumentException();
            }
            return statuses;
        } catch (IllegalArgumentException exception) {
            throw error(ProjectErrorCode.VALIDATION_ERROR,
                    "status는 PENDING 또는 APPROVED만 조회할 수 있습니다.");
        }
    }

    private void validateRequest(ProjectContributionGetListRequest request) {
        if (request == null || request.projectId() == null || request.userId() == null
                || request.size() < 1 || request.size() > 100
                || (request.cursor() != null && request.cursor() < 1)) {
            throw error(ProjectErrorCode.VALIDATION_ERROR, "조회 조건이 올바르지 않습니다.");
        }
    }

    private ProjectApplicationException error(ProjectErrorCode code, String message) {
        return new ProjectApplicationException(code, message);
    }
}
