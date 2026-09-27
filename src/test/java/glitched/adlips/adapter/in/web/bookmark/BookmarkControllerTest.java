package glitched.adlips.adapter.in.web.bookmark;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.bookmark.BookmarkApplicationException;
import glitched.adlips.application.bookmark.BookmarkErrorCode;
import glitched.adlips.application.bookmark.BookmarkedPostListResult;
import glitched.adlips.application.bookmark.BookmarkedShortPage;
import glitched.adlips.application.bookmark.GetBookmarkedPostsUseCase;
import glitched.adlips.application.bookmark.GetBookmarkedShortsUseCase;
import glitched.adlips.application.bookmark.port.out.BookmarkedPostItem;
import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookmarkController.class)
@Import(SecurityConfig.class)
class BookmarkControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetBookmarkedPostsUseCase getBookmarkedPostsUseCase;
    @MockitoBean GetBookmarkedShortsUseCase getBookmarkedShortsUseCase;
    @MockitoBean AuthenticatedUserResolver authenticatedUserResolver;
    @MockitoBean AccessTokenPort accessTokenPort;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(accessTokenPort.verify(validToken)).thenReturn(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 북마크한_게시글_목록을_반환한다() throws Exception {
        BookmarkedPostItem item = new BookmarkedPostItem(
                501L, 10L, "음악", 2L, "writer", null, "제목", "가".repeat(101), null, 3, 1, 0,
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 27, 10, 0));
        when(getBookmarkedPostsUseCase.execute(1L, 0, 20))
                .thenReturn(new BookmarkedPostListResult(0, 20, 1L, List.of(item)));

        mockMvc.perform(get("/api/v1/users/me/bookmarks/posts").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.posts[0].postId").value(501))
                .andExpect(jsonPath("$.data.posts[0].gallery.name").value("음악"))
                .andExpect(jsonPath("$.data.posts[0].writer.nickname").value("writer"))
                .andExpect(jsonPath("$.data.posts[0].contentPreview").value("가".repeat(100) + "…"))
                .andExpect(jsonPath("$.data.posts[0].bookmarkedAt").value("2026-09-27T10:00:00"));
    }

    @Test
    void 북마크한_숏폼_목록을_커서_페이지로_반환한다() throws Exception {
        BookmarkedShortItem item = new BookmarkedShortItem(12L, 202L, "숏폼", "https://cdn.test/album.png",
                "COMPLETED", 2L, "writer", null, 7, 3L, LocalDateTime.of(2026, 9, 27, 10, 0));
        when(getBookmarkedShortsUseCase.execute(1L, 30L, 5))
                .thenReturn(new BookmarkedShortPage(List.of(item), 12L, true, 5));

        mockMvc.perform(get("/api/v1/users/me/bookmarks/shorts")
                        .param("cursor", "30").param("size", "5")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].shortId").value(202))
                .andExpect(jsonPath("$.data.items[0].title").value("숏폼"))
                .andExpect(jsonPath("$.data.items[0].albumImageUrl").value("https://cdn.test/album.png"))
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("writer"))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(7))
                .andExpect(jsonPath("$.data.items[0].commentCount").value(3))
                .andExpect(jsonPath("$.data.page.nextCursor").value(12))
                .andExpect(jsonPath("$.data.page.hasMore").value(true))
                .andExpect(jsonPath("$.data.page.size").value(5));
    }

    @Test
    void 잘못된_페이지면_400을_반환한다() throws Exception {
        when(getBookmarkedPostsUseCase.execute(1L, -1, 20)).thenThrow(
                new BookmarkApplicationException(BookmarkErrorCode.INVALID_INPUT_VALUE, "페이지 번호와 크기를 확인해 주세요."));

        mockMvc.perform(get("/api/v1/users/me/bookmarks/posts").param("page", "-1")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/bookmarks/posts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me/bookmarks/shorts")).andExpect(status().isUnauthorized());
    }
}
