package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortsCompositionDetailController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.GetShortsCompositionDetailUseCase;
import glitched.adlips.application.shorts.ShortsCompositionApplicationException;
import glitched.adlips.application.shorts.ShortsCompositionErrorCode;
import glitched.adlips.application.shorts.ShortsCompositionDetailResult;
import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.global.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortsCompositionDetailController.class)
@Import(SecurityConfig.class)
class ShortsCompositionDetailControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetShortsCompositionDetailUseCase getShortsCompositionDetailUseCase;
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
    void 유효한_요청시_200과_작곡_상세_정보를_반환한다() throws Exception {
        when(getShortsCompositionDetailUseCase.execute(12L)).thenReturn(new ShortsCompositionDetailResult(
                12L, "밤하늘 위 멜로디", List.of(
                        new ShortsComposerQueryItem(3L, "guitar_moon", "https://cdn.example.com/profiles/3.png",
                                "기타", "메인 기타 리프를 만들었습니다."),
                        new ShortsComposerQueryItem(7L, "vocal_wave", "https://cdn.example.com/profiles/7.png",
                                "보컬", "후렴 멜로디와 보컬 라인을 추가했습니다.")
                )));

        mockMvc.perform(get("/api/v1/shorts/12/composition-details")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("작곡 상세 정보 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.shortId").value(12))
                .andExpect(jsonPath("$.data.title").value("밤하늘 위 멜로디"))
                .andExpect(jsonPath("$.data.details.length()").value(2))
                .andExpect(jsonPath("$.data.details[0].userId").value(3))
                .andExpect(jsonPath("$.data.details[0].nickname").value("guitar_moon"))
                .andExpect(jsonPath("$.data.details[0].role").value("기타"))
                .andExpect(jsonPath("$.data.details[1].userId").value(7));
    }

    @Test
    void 참여자가_없으면_details가_빈_배열이다() throws Exception {
        when(getShortsCompositionDetailUseCase.execute(12L)).thenReturn(new ShortsCompositionDetailResult(
                12L, "밤하늘 위 멜로디", List.of()));

        mockMvc.perform(get("/api/v1/shorts/12/composition-details")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.details.length()").value(0));
    }

    @Test
    void 존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(getShortsCompositionDetailUseCase.execute(999L)).thenThrow(
                new ShortsCompositionApplicationException(
                        ShortsCompositionErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다."));

        mockMvc.perform(get("/api/v1/shorts/999/composition-details")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 숏폼입니다."));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/shorts/12/composition-details"))
                .andExpect(status().isUnauthorized());
    }
}
