package glitched.adlips.adapter.in.web;

import glitched.adlips.adapter.in.web.shorts.ShortsController;
import glitched.adlips.application.shorts.AuthorSummary;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.ShortSummary;
import glitched.adlips.application.shorts.ShortsPage;
import glitched.adlips.application.shorts.ShortsSource;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.domain.shorts.ShortStatus;
import java.time.LocalDateTime;
import java.util.List;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortsController.class)
@Import(SecurityConfig.class)
class ShortsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GetShortsUseCase getShortsUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    @BeforeEach
    void setUpDefaultPage() {
        when(authenticatedUserResolver.resolveOptionalUserId(isNull())).thenReturn(null);
        when(getShortsUseCase.get(
                nullable(Long.class),
                any(ShortsSource.class),
                any(ShortStatus.class),
                anyInt(),
                nullable(Long.class)
        )).thenReturn(emptyShortsPage(20));
    }

    @Test
    void 상태_파라미터가_없으면_COMPLETED를_기본값으로_사용한다() throws Exception {
        mockMvc.perform(get("/api/v1/shorts"))
                .andExpect(status().isOk());

        verify(getShortsUseCase).get(
                isNull(), eq(ShortsSource.SHORTS_FEED), eq(ShortStatus.COMPLETED), eq(20), isNull());
    }

    @Test
    void GET_shorts_미인증_요청시_200을_반환한다() throws Exception {
        when(getShortsUseCase.get(isNull(), eq(ShortsSource.SHORTS_FEED), eq(ShortStatus.COMPLETED), eq(20), isNull()))
                .thenReturn(emptyShortsPage(20));

        mockMvc.perform(get("/api/v1/shorts").param("completionStatus", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("숏폼 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").exists());
    }

    @Test
    void GET_shorts_결과가_있으면_items와_page를_반환한다() throws Exception {
        ShortsPage page = singleItemPage();
        when(getShortsUseCase.get(isNull(), eq(ShortsSource.SHORTS_FEED), eq(ShortStatus.COMPLETED), eq(20), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/shorts").param("completionStatus", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].shortId").value(12))
                .andExpect(jsonPath("$.data.items[0].title").value("밤하늘 위 멜로디"))
                .andExpect(jsonPath("$.data.items[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.items[0].isDisliked").value(false))
                .andExpect(jsonPath("$.data.items[0].isBookmarked").value(false))
                .andExpect(jsonPath("$.data.page.hasMore").value(true))
                .andExpect(jsonPath("$.data.page.nextCursor").value(12))
                .andExpect(jsonPath("$.data.page.size").value(20));
    }

    @Test
    void cursor_파라미터를_전달하면_유스케이스에_전달된다() throws Exception {
        when(getShortsUseCase.get(eq(50L), any(), any(), anyInt(), isNull()))
                .thenReturn(emptyShortsPage(20));

        mockMvc.perform(get("/api/v1/shorts")
                        .param("completionStatus", "COMPLETED")
                        .param("cursor", "50"))
                .andExpect(status().isOk());

        verify(getShortsUseCase).get(eq(50L), any(), any(), anyInt(), isNull());
    }

    @Test
    void size_파라미터를_전달하면_유스케이스에_전달된다() throws Exception {
        when(getShortsUseCase.get(isNull(), any(), any(), eq(5), isNull()))
                .thenReturn(emptyShortsPage(5));

        mockMvc.perform(get("/api/v1/shorts")
                        .param("completionStatus", "COMPLETED")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(getShortsUseCase).get(isNull(), any(), any(), eq(5), isNull());
    }

    @Test
    void completionStatus_IN_PROGRESS_전달하면_유스케이스에_전달된다() throws Exception {
        when(getShortsUseCase.get(isNull(), any(), eq(ShortStatus.IN_PROGRESS), anyInt(), isNull()))
                .thenReturn(emptyShortsPage(20));

        mockMvc.perform(get("/api/v1/shorts").param("completionStatus", "IN_PROGRESS"))
                .andExpect(status().isOk());

        verify(getShortsUseCase).get(isNull(), any(), eq(ShortStatus.IN_PROGRESS), anyInt(), isNull());
    }

    @Test
    void 기존_status_IN_PROGRESS도_유스케이스에_전달된다() throws Exception {
        when(getShortsUseCase.get(isNull(), any(), eq(ShortStatus.IN_PROGRESS), anyInt(), isNull()))
                .thenReturn(emptyShortsPage(20));

        mockMvc.perform(get("/api/v1/shorts").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        verify(getShortsUseCase).get(isNull(), any(), eq(ShortStatus.IN_PROGRESS), anyInt(), isNull());
    }

    @Test
    void author_정보가_응답에_포함된다() throws Exception {
        ShortsPage page = singleItemPage();
        when(getShortsUseCase.get(isNull(), eq(ShortsSource.SHORTS_FEED), eq(ShortStatus.COMPLETED), eq(20), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/shorts").param("completionStatus", "COMPLETED"))
                .andExpect(jsonPath("$.data.items[0].author.userId").value(123))
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("guitar_moon"));
    }

    @Test
    void 인증_토큰의_사용자_ID를_유스케이스에_전달한다() throws Exception {
        when(authenticatedUserResolver.resolveOptionalUserId("Bearer token")).thenReturn(99L);
        when(getShortsUseCase.get(
                isNull(), eq(ShortsSource.SHORTS_FEED), eq(ShortStatus.COMPLETED), eq(20), eq(99L)))
                .thenReturn(emptyShortsPage(20));

        mockMvc.perform(get("/api/v1/shorts")
                        .param("completionStatus", "COMPLETED")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());

        verify(getShortsUseCase).get(
                isNull(), eq(ShortsSource.SHORTS_FEED), eq(ShortStatus.COMPLETED), eq(20), eq(99L));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ShortsPage emptyShortsPage(int size) {
        return new ShortsPage(List.of(), null, false, size);
    }

    private ShortsPage singleItemPage() {
        ShortSummary item = new ShortSummary(
                12L, 8L, "밤하늘 위 멜로디",
                "https://cdn.example.com/shorts/12.mp4",
                "https://cdn.example.com/albums/12.png",
                "COMPLETED", 1200, 128, 4, 23L, 5,
                false, false, false,
                new AuthorSummary(123L, "guitar_moon", null),
                List.of(),
                LocalDateTime.of(2026, 6, 25, 12, 3)
        );
        return new ShortsPage(List.of(item), 12L, true, 20);
    }
}
