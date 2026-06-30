package glitched.adlips.application.shorts;

import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.ShortCommentRepository;
import glitched.adlips.application.port.ShortFormRepository;
import glitched.adlips.application.port.ShortInteractionRepository;
import glitched.adlips.application.port.ShortParticipantRepository;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortParticipant;
import glitched.adlips.domain.shorts.ShortStatus;
import glitched.adlips.domain.user.Profile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GetShortsUseCase {

    private final ShortFormRepository shortFormRepository;
    private final ShortParticipantRepository shortParticipantRepository;
    private final ShortInteractionRepository shortInteractionRepository;
    private final ShortCommentRepository shortCommentRepository;
    private final ProfileRepository profileRepository;

    public GetShortsUseCase(
            ShortFormRepository shortFormRepository,
            ShortParticipantRepository shortParticipantRepository,
            ShortInteractionRepository shortInteractionRepository,
            ShortCommentRepository shortCommentRepository,
            ProfileRepository profileRepository
    ) {
        this.shortFormRepository = shortFormRepository;
        this.shortParticipantRepository = shortParticipantRepository;
        this.shortInteractionRepository = shortInteractionRepository;
        this.shortCommentRepository = shortCommentRepository;
        this.profileRepository = profileRepository;
    }

    public ShortsPage get(Long cursor, ShortStatus status, int size, Long currentUserId) {
        List<ShortForm> fetched = shortFormRepository.findByCursor(cursor, status, size + 1);
        boolean hasMore = fetched.size() > size;
        List<ShortForm> shorts = hasMore ? fetched.subList(0, size) : fetched;

        if (shorts.isEmpty()) {
            return new ShortsPage(List.of(), null, false, size);
        }

        List<Long> shortIds = shorts.stream().map(ShortForm::getId).toList();

        Map<Long, Profile> profileByUserId = loadProfiles(shorts);
        Map<Long, List<ShortParticipant>> participantsByShortId = loadParticipants(shortIds, profileByUserId);
        Map<Long, Long> commentCounts = shortCommentRepository.countByShortIds(shortIds);

        Set<Long> likedIds = Set.of();
        Set<Long> dislikedIds = Set.of();
        Set<Long> bookmarkedIds = Set.of();
        if (currentUserId != null) {
            likedIds = shortInteractionRepository.findLikedShortIds(currentUserId, shortIds);
            dislikedIds = shortInteractionRepository.findDislikedShortIds(currentUserId, shortIds);
            bookmarkedIds = shortInteractionRepository.findBookmarkedShortIds(currentUserId, shortIds);
        }

        final Set<Long> finalLiked = likedIds;
        final Set<Long> finalDisliked = dislikedIds;
        final Set<Long> finalBookmarked = bookmarkedIds;

        List<ShortSummary> items = shorts.stream().map(s -> {
            Profile authorProfile = profileByUserId.get(s.getUser().getId());
            AuthorSummary author = new AuthorSummary(
                    s.getUser().getId(),
                    authorProfile != null ? authorProfile.getNickname() : null,
                    null
            );

            List<ParticipantSummary> participants = participantsByShortId
                    .getOrDefault(s.getId(), List.of())
                    .stream()
                    .map(p -> {
                        Profile pp = profileByUserId.get(p.getUser().getId());
                        return new ParticipantSummary(
                                p.getUser().getId(),
                                pp != null ? pp.getNickname() : null,
                                p.getRole()
                        );
                    })
                    .toList();

            return new ShortSummary(
                    s.getId(),
                    s.getProject() != null ? s.getProject().getId() : null,
                    s.getTitle(),
                    s.getMediaUrl(),
                    s.getAlbumImage(),
                    s.getStatus().name(),
                    s.getViewCount(),
                    s.getLikeCount(),
                    s.getDislikeCount(),
                    commentCounts.getOrDefault(s.getId(), 0L),
                    s.getPrCount(),
                    finalLiked.contains(s.getId()),
                    finalDisliked.contains(s.getId()),
                    finalBookmarked.contains(s.getId()),
                    author,
                    participants,
                    s.getCreatedAt()
            );
        }).toList();

        Long nextCursor = items.getLast().shortId();
        return new ShortsPage(items, nextCursor, hasMore, size);
    }

    private Map<Long, Profile> loadProfiles(List<ShortForm> shorts) {
        List<Long> authorIds = shorts.stream().map(s -> s.getUser().getId()).toList();
        Map<Long, Profile> profileByUserId = new HashMap<>();
        profileRepository.findByUserIds(authorIds).forEach(p -> profileByUserId.put(p.getId(), p));
        return profileByUserId;
    }

    private Map<Long, List<ShortParticipant>> loadParticipants(
            List<Long> shortIds, Map<Long, Profile> profileByUserId) {
        List<ShortParticipant> allParticipants = shortParticipantRepository.findByShortIds(shortIds);

        List<Long> missingIds = allParticipants.stream()
                .map(p -> p.getUser().getId())
                .filter(id -> !profileByUserId.containsKey(id))
                .distinct()
                .toList();
        if (!missingIds.isEmpty()) {
            profileRepository.findByUserIds(missingIds).forEach(p -> profileByUserId.put(p.getId(), p));
        }

        return allParticipants.stream()
                .collect(Collectors.groupingBy(p -> p.getShorts().getId()));
    }
}
