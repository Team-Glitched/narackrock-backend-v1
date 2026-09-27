package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.GetPostShareUseCase;
import glitched.adlips.application.community.PostApplicationException;
import glitched.adlips.application.community.PostErrorCode;
import glitched.adlips.application.community.PostShareResult;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostShareController.class)
@Import(SecurityConfig.class)
class PostShareControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetPostShareUseCase getPostShareUseCase;
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
    void 게시글_공유_링크를_반환한다() throws Exception {
        when(getPostShareUseCase.execute(501L)).thenReturn(new PostShareResult(501L, "https://app.example.com/posts/501"));

        mockMvc.perform(post("/api/v1/posts/501/share").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(501))
                .andExpect(jsonPath("$.data.shareUrl").value("https://app.example.com/posts/501"));
    }

    @Test
    void 없는_게시글이면_404를_반환한다() throws Exception {
        when(getPostShareUseCase.execute(999L)).thenThrow(
                new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다."));

        mockMvc.perform(post("/api/v1/posts/999/share").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void 인증없는_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/posts/501/share")).andExpect(status().isUnauthorized());
    }
}
