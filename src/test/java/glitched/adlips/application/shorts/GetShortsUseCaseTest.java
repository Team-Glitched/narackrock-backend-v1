package glitched.adlips.application.shorts;

import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.ShortCommentRepository;
import glitched.adlips.application.port.ShortFormRepository;
import glitched.adlips.application.port.ShortInteractionRepository;
import glitched.adlips.application.port.ShortParticipantRepository;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortStatus;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetShortsUseCaseTest {

    @Mock ShortFormRepository shortFormRepository;
    @Mock ShortParticipantRepository shortParticipantRepository;
    @Mock ShortInteractionRepository shortInteractionRepository;
    @Mock ShortCommentRepository shortCommentRepository;
    @Mock ProfileRepository profileRepository;

    GetShortsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetShortsUseCase(
                shortFormRepository, shortParticipantRepository,
                shortInteractionRepository, shortCommentRepository, profileRepository
        );
    }

    @Test
    void 결과가_없으면_빈_페이지를_반환한다() {
        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), anyInt()))
                .thenReturn(List.of());

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 20, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void size보다_많은_결과가_있으면_hasMore가_true이고_size만큼만_반환한다() {
        List<ShortForm> twentyOneShorts = buildShorts(21);
        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), eq(21)))
                .thenReturn(twentyOneShorts);
        when(shortParticipantRepository.findByShortIds(anyList())).thenReturn(List.of());
        when(shortCommentRepository.countByShortIds(anyList())).thenReturn(Map.of());
        when(profileRepository.findByUserIds(anyList())).thenReturn(List.of());

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 20, null);

        assertThat(page.items()).hasSize(20);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(twentyOneShorts.get(19).getId());
    }

    @Test
    void 결과가_size와_같으면_hasMore가_false이다() {
        List<ShortForm> fiveShorts = buildShorts(5);
        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), eq(6)))
                .thenReturn(fiveShorts);
        when(shortParticipantRepository.findByShortIds(anyList())).thenReturn(List.of());
        when(shortCommentRepository.countByShortIds(anyList())).thenReturn(Map.of());
        when(profileRepository.findByUserIds(anyList())).thenReturn(List.of());

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 5, null);

        assertThat(page.items()).hasSize(5);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void 미인증_사용자는_isLiked_isDisliked_isBookmarked가_모두_false이다() {
        List<ShortForm> shorts = buildShorts(1);
        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), anyInt()))
                .thenReturn(shorts);
        when(shortParticipantRepository.findByShortIds(anyList())).thenReturn(List.of());
        when(shortCommentRepository.countByShortIds(anyList())).thenReturn(Map.of());
        when(profileRepository.findByUserIds(anyList())).thenReturn(List.of());

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 20, null);

        ShortSummary item = page.items().getFirst();
        assertThat(item.isLiked()).isFalse();
        assertThat(item.isDisliked()).isFalse();
        assertThat(item.isBookmarked()).isFalse();
        verify(shortInteractionRepository, never()).findLikedShortIds(any(), any());
    }

    @Test
    void 인증_사용자는_좋아요_싫어요_북마크_상태가_반영된다() {
        List<ShortForm> shorts = buildShorts(2);
        Long userId = 99L;
        Long likedShortId = shorts.get(0).getId();

        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), anyInt()))
                .thenReturn(shorts);
        when(shortParticipantRepository.findByShortIds(anyList())).thenReturn(List.of());
        when(shortCommentRepository.countByShortIds(anyList())).thenReturn(Map.of());
        when(profileRepository.findByUserIds(anyList())).thenReturn(List.of());
        when(shortInteractionRepository.findLikedShortIds(eq(userId), anyList()))
                .thenReturn(Set.of(likedShortId));
        when(shortInteractionRepository.findDislikedShortIds(eq(userId), anyList()))
                .thenReturn(Set.of());
        when(shortInteractionRepository.findBookmarkedShortIds(eq(userId), anyList()))
                .thenReturn(Set.of());

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 20, userId);

        assertThat(page.items().get(0).isLiked()).isTrue();
        assertThat(page.items().get(1).isLiked()).isFalse();
    }

    @Test
    void cursor가_있으면_해당_cursor값으로_조회한다() {
        Long cursor = 50L;
        when(shortFormRepository.findByCursor(eq(cursor), eq(ShortStatus.COMPLETED), anyInt()))
                .thenReturn(List.of());

        useCase.get(cursor, ShortStatus.COMPLETED, 20, null);

        verify(shortFormRepository).findByCursor(eq(cursor), eq(ShortStatus.COMPLETED), eq(21));
    }

    @Test
    void commentCount가_응답에_포함된다() {
        List<ShortForm> shorts = buildShorts(1);
        Long shortId = shorts.getFirst().getId();

        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), anyInt()))
                .thenReturn(shorts);
        when(shortParticipantRepository.findByShortIds(anyList())).thenReturn(List.of());
        when(shortCommentRepository.countByShortIds(anyList())).thenReturn(Map.of(shortId, 7L));
        when(profileRepository.findByUserIds(anyList())).thenReturn(List.of());

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 20, null);

        assertThat(page.items().getFirst().commentCount()).isEqualTo(7L);
    }

    @Test
    void author_닉네임이_응답에_포함된다() {
        User user = buildUser(10L);
        Profile profile = buildProfile(user, "guitar_moon");
        ShortForm short1 = buildShortWithUser(1L, user);

        when(shortFormRepository.findByCursor(isNull(), eq(ShortStatus.COMPLETED), anyInt()))
                .thenReturn(List.of(short1));
        when(shortParticipantRepository.findByShortIds(anyList())).thenReturn(List.of());
        when(shortCommentRepository.countByShortIds(anyList())).thenReturn(Map.of());
        when(profileRepository.findByUserIds(anyList())).thenReturn(List.of(profile));

        ShortsPage page = useCase.get(null, ShortStatus.COMPLETED, 20, null);

        assertThat(page.items().getFirst().author().nickname()).isEqualTo("guitar_moon");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<ShortForm> buildShorts(int count) {
        return java.util.stream.LongStream.rangeClosed(1, count)
                .mapToObj(id -> buildShortWithUser(id, buildUser(id + 100)))
                .toList();
    }

    private ShortForm buildShortWithUser(Long shortId, User user) {
        try {
            ShortForm s = instantiateShortForm();
            setField(s, "id", shortId);
            setField(s, "user", user);
            setField(s, "title", "제목 " + shortId);
            setField(s, "mediaUrl", "https://cdn.example.com/" + shortId + ".mp4");
            setField(s, "status", ShortStatus.COMPLETED);
            setField(s, "createdAt", LocalDateTime.now());
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User buildUser(Long userId) {
        try {
            User u = instantiateUser();
            setField(u, "id", userId);
            setField(u, "email", "user" + userId + "@test.com");
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Profile buildProfile(User user, String nickname) {
        try {
            Profile p = new Profile(user, nickname);
            setField(p, "id", user.getId());
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ShortForm instantiateShortForm() throws Exception {
        var constructor = ShortForm.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private User instantiateUser() throws Exception {
        var constructor = User.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in " + target.getClass());
    }
}
