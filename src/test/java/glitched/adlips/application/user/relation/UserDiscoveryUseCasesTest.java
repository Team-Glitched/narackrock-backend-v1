package glitched.adlips.application.user.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glitched.adlips.application.user.relation.FollowUseCasesTest.EmptyMedia;
import glitched.adlips.application.user.relation.FollowUseCasesTest.SocialStore;
import glitched.adlips.application.user.relation.dto.request.RecommendedUserGetListRequest;
import glitched.adlips.application.user.relation.dto.request.UserSearchRequest;
import glitched.adlips.application.user.relation.dto.response.RecommendedUserGetListResponse;
import glitched.adlips.application.user.relation.dto.response.UserSearchResponse;
import glitched.adlips.application.user.relation.model.RecommendType;
import glitched.adlips.application.user.relation.usecase.RecommendedUserGetListUseCase;
import glitched.adlips.application.user.relation.usecase.UserSearchUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDiscoveryUseCasesTest {

    private SocialStore store;
    private UserSearchUseCase userSearchUseCase;
    private RecommendedUserGetListUseCase recommendedUserGetListUseCase;

    @BeforeEach
    void setUp() {
        store = new SocialStore();
        store.addUser(1L, "me", 0);
        store.addUser(2L, "guitar_friend", 10);
        store.addUser(3L, "jazz_creator", 20);
        store.addUser(4L, "popular", 100);
        userSearchUseCase = new UserSearchUseCase(store, store, new EmptyMedia());
        recommendedUserGetListUseCase = new RecommendedUserGetListUseCase(
                store, store, new EmptyMedia()
        );
    }

    @Test
    void searchesProfilesAndMarksFollowingState() {
        store.save(1L, 2L);

        UserSearchResponse result = userSearchUseCase.execute(new UserSearchRequest(1L, "기타", 0, 20));

        assertEquals("기타", result.searchKeyword());
        assertEquals(4, result.totalResultCount());
        assertTrue(result.users().stream()
                .filter(user -> user.userId().equals(2L))
                .findFirst()
                .orElseThrow()
                .isFollowing());
    }

    @Test
    void recommendsFriendOfFriendBeforePopularFallback() {
        store.save(1L, 2L);
        store.save(2L, 3L);

        RecommendedUserGetListResponse result = recommendedUserGetListUseCase.execute(
                new RecommendedUserGetListRequest(1L, 0, 20)
        );

        assertFalse(result.fallbackTriggered());
        assertEquals(1, result.totalCount());
        assertEquals(3L, result.recommendedUsers().getFirst().userId());
        assertEquals(RecommendType.FRIEND_OF_FRIEND, result.recommendedUsers().getFirst().recommendType());
    }

    @Test
    void fallsBackToPopularUsersWhenPersonalizedCandidatesAreEmpty() {
        RecommendedUserGetListResponse result = recommendedUserGetListUseCase.execute(
                new RecommendedUserGetListRequest(1L, 0, 20)
        );

        assertTrue(result.fallbackTriggered());
        assertEquals(4L, result.recommendedUsers().getFirst().userId());
        assertEquals(RecommendType.POPULAR_CREATOR, result.recommendedUsers().getFirst().recommendType());
    }
}
