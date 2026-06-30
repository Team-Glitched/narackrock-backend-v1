package glitched.adlips.application.user.relation;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.relation.RecommendationResult.RecommendedUser;
import glitched.adlips.application.user.relation.UserSearchResult.SearchUser;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UserDiscoveryService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ProfileQueryPort profileQuery;
    private final FollowRepositoryPort followRepository;
    private final MediaFileRepositoryPort mediaFileRepository;

    public UserDiscoveryService(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository
    ) {
        this.profileQuery = profileQuery;
        this.followRepository = followRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    public UserSearchResult search(Long requesterId, String keyword, int page, int size) {
        validatePage(page, size);
        if (keyword == null || keyword.isBlank()) {
            throw new UserApplicationException(
                    UserErrorCode.VALIDATION_ERROR,
                    "검색어는 필수 입력 사항입니다."
            );
        }
        String normalizedKeyword = keyword.trim();
        PageResult<Profile> result = profileQuery.search(normalizedKeyword, page, size);
        List<SearchUser> users = result.content().stream()
                .map(profile -> new SearchUser(
                        profile.getUserId(),
                        profile.getNickname(),
                        imageUrl(profile),
                        profile.getPrimaryInstrument(),
                        profile.getExplanation(),
                        !profile.getUserId().equals(requesterId)
                                && followRepository.exists(requesterId, profile.getUserId())
                ))
                .toList();
        return new UserSearchResult(normalizedKeyword, result.totalCount(), users);
    }

    public RecommendationResult recommend(Long requesterId, int page, int size) {
        validatePage(page, size);
        Set<Long> followingIds = new LinkedHashSet<>(followRepository.findFollowingIds(requesterId));
        Set<Long> candidates = new LinkedHashSet<>();
        for (Long followingId : followingIds) {
            candidates.addAll(followRepository.findFollowingIds(followingId));
        }
        candidates.remove(requesterId);
        candidates.removeAll(followingIds);

        List<Profile> personalized = profileQuery.findAllByUserIds(candidates).stream()
                .sorted(Comparator.comparingInt(Profile::getFollowerCount).reversed())
                .toList();
        if (!personalized.isEmpty()) {
            List<Profile> pageContent = page(personalized, page, size);
            return new RecommendationResult(
                    personalized.size(),
                    false,
                    recommendedUsers(pageContent, requesterId, RecommendType.FRIEND_OF_FRIEND)
            );
        }

        Set<Long> excludedIds = new LinkedHashSet<>(followingIds);
        excludedIds.add(requesterId);
        PageResult<Profile> fallback = profileQuery.findPopularExcluding(excludedIds, page, size);
        return new RecommendationResult(
                fallback.totalCount(),
                true,
                recommendedUsers(fallback.content(), requesterId, RecommendType.POPULAR_CREATOR)
        );
    }

    private List<RecommendedUser> recommendedUsers(
            Collection<Profile> profiles,
            Long requesterId,
            RecommendType type
    ) {
        String explanation = type == RecommendType.FRIEND_OF_FRIEND
                ? "함께 아는 사용자가 있습니다."
                : "최근 활동이 활발한 작곡가입니다.";
        return profiles.stream()
                .map(profile -> new RecommendedUser(
                        profile.getUserId(),
                        profile.getNickname(),
                        imageUrl(profile),
                        profile.getPrimaryInstrument(),
                        type,
                        explanation,
                        followRepository.exists(requesterId, profile.getUserId())
                ))
                .toList();
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
            throw new UserApplicationException(
                    UserErrorCode.VALIDATION_ERROR,
                    "페이지 번호와 크기를 확인해 주세요."
            );
        }
    }
}
