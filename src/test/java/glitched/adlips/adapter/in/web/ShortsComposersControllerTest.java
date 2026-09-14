package glitched.adlips.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.shorts.ShortsComposersController;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.shorts.GetShortsComposersUseCase;
import glitched.adlips.application.shorts.ShortsComposersApplicationException;
import glitched.adlips.application.shorts.ShortsComposersErrorCode;
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

@WebMvcTest(ShortsComposersController.class)
@Import(SecurityConfig.class)
class ShortsComposersControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetShortsComposersUseCase getShortsComposersUseCase;
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
    void 유효한_요청시_200과_참여자_목록을_반환한다() throws Exception {
        when(getShortsComposersUseCase.execute(12L)).thenReturn(List.of(
                new ShortsComposerQueryItem(3L, "guitar_moon", "https://cdn.example.com/profiles/3.png",
                        "기타", "메인 기타 리프를 만들었습니다."),
                new ShortsComposerQueryItem(7L, "vocal_wave", "https://cdn.example.com/profiles/7.png",
                        "보컬", "후렴 멜로디와 보컬 라인을 추가했습니다.")
        ));

        mockMvc.perform(get("/api/v1/shorts/12/composers")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("참여자 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(3))
                .andExpect(jsonPath("$.data[0].nickname").value("guitar_moon"))
                .andExpect(jsonPath("$.data[0].profileImageUrl").value("https://cdn.example.com/profiles/3.png"))
                .andExpect(jsonPath("$.data[0].role").value("기타"))
                .andExpect(jsonPath("$.data[0].description").value("메인 기타 리프를 만들었습니다."))
                .andExpect(jsonPath("$.data[1].userId").value(7));
    }

    @Test
    void 참여자가_없으면_200과_빈_배열을_반환한다() throws Exception {
        when(getShortsComposersUseCase.execute(12L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/shorts/12/composers")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 존재하지_않는_숏폼이면_404를_반환한다() throws Exception {
        when(getShortsComposersUseCase.execute(999L)).thenThrow(
                new ShortsComposersApplicationException(
                        ShortsComposersErrorCode.SHORTS_NOT_FOUND, "해당 곡을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/shorts/999/composers")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORTS_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 곡을 찾을 수 없습니다."));
    }

    @Test
    void 인증없는_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/shorts/12/composers"))
                .andExpect(status().isUnauthorized());
    }
}
