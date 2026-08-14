package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.user.relation.dto.request.RecommendedUserGetListRequest;
import glitched.adlips.application.user.relation.dto.response.RecommendedUserGetListResponse;
import glitched.adlips.application.user.relation.dto.response.RecommendedUserGetListResponse.RecommendedUserResponse;
import glitched.adlips.application.user.relation.model.PageResult;
import glitched.adlips.application.user.relation.model.RecommendType;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class RecommendedUserGetListUseCase {
    private static final int MAX_PAGE_SIZE = 100;

    private final ProfileQueryPort profileQuery;
    private final FollowRepositoryPort followRepository;
    private final CollaborationUserQueryPort collaborationUserQueryPort;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final TransactionRunner transactionRunner;

    public RecommendedUserGetListUseCase(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            CollaborationUserQueryPort collaborationUserQueryPort,
            MediaFileRepositoryPort mediaFileRepository
    ) {
        this(profileQuery, followRepository, mediaFileRepository, TransactionRunner.direct());
    }

    public RecommendedUserGetListUseCase(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        this.profileQuery = profileQuery;
        this.followRepository = followRepository;
        this.collaborationUserQueryPort = collaborationUserQueryPort;
        this.mediaFileRepository = mediaFileRepository;
        this.transactionRunner = transactionRunner;
    }

    public RecommendedUserGetListResponse execute(RecommendedUserGetListRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private RecommendedUserGetListResponse executeInternal(RecommendedUserGetListRequest request) {
        Long requesterId = request.requesterId();
        int page = request.page();
        int size = request.size();
        validatePage(page, size);
        Set<Long> followingIds = new LinkedHashSet<>(followRepository.findFollowingIds(requesterId));
        Map<Long, RecommendType> candidateTypes = new LinkedHashMap<>();
        for (Long followingId : followingIds) {
            for (Long candidateId : followRepository.findFollowingIds(followingId)) {
                candidateTypes.putIfAbsent(candidateId, RecommendType.FRIEND_OF_FRIEND);
            }
        }
        for (Long collaboratorId : collaborationUserQueryPort.findCollaboratorIds(requesterId)) {
            candidateTypes.putIfAbsent(collaboratorId, RecommendType.COLLABORATOR);
        }
        candidateTypes.remove(requesterId);
        followingIds.forEach(candidateTypes::remove);

        List<Profile> personalized = profileQuery.findAllByUserIds(candidateTypes.keySet()).stream()
                .sorted(Comparator.comparingInt(Profile::getFollowerCount).reversed())
                .toList();
        if (!personalized.isEmpty()) {
            return new RecommendedUserGetListResponse(
                    personalized.size(),
                    false,
                    recommendedUsers(page(personalized, page, size), requesterId, candidateTypes)
            );
        }

        Set<Long> excludedIds = new LinkedHashSet<>(followingIds);
        excludedIds.add(requesterId);
        PageResult<Profile> fallback = profileQuery.findPopularExcluding(excludedIds, page, size);
        return new RecommendedUserGetListResponse(
                fallback.totalCount(),
                true,
                recommendedUsers(fallback.content(), requesterId, RecommendType.POPULAR_CREATOR)
        );
    }

    private List<RecommendedUserResponse> recommendedUsers(
            Collection<Profile> profiles,
            Long requesterId,
            Map<Long, RecommendType> candidateTypes
    ) {
        return profiles.stream()
                .map(profile -> recommendedUser(profile, requesterId, candidateTypes.get(profile.getUserId())))
                .toList();
    }

    private List<RecommendedUserResponse> recommendedUsers(
            Collection<Profile> profiles,
            Long requesterId,
            RecommendType type
    ) {
        return profiles.stream()
                .map(profile -> recommendedUser(profile, requesterId, type))
                .toList();
    }

    private RecommendedUserResponse recommendedUser(Profile profile, Long requesterId, RecommendType type) {
        return new RecommendedUserResponse(
                profile.getUserId(),
                profile.getNickname(),
                imageUrl(profile),
                profile.getPrimaryInstrument(),
                type,
                explanation(type),
                followRepository.exists(requesterId, profile.getUserId())
        );
    }

    private String explanation(RecommendType type) {
        return switch (type) {
            case FRIEND_OF_FRIEND -> "함께 아는 사용자가 있습니다.";
            case COLLABORATOR -> "함께 작업한 이력이 있습니다.";
            case POPULAR_CREATOR -> "최근 활동이 활발한 작곡가입니다.";
        };
    }

    private String imageUrl(Profile profile) {
        if (profile.getProfileImageFileId() == null) {
            return null;
        }
        return mediaFileRepository.findById(profile.getProfileImageFileId())
                .map(MediaFile::getFileUrl)
                .orElse(null);
    }

    private List<Profile> page(List<Profile> values, int page, int size) {
        int from = Math.min(page * size, values.size());
        int to = Math.min(from + size, values.size());
        return values.subList(from, to);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요.");
        }
    }
}
