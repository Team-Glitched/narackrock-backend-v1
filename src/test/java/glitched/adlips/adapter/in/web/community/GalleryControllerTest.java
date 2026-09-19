package glitched.adlips.adapter.in.web.community;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.community.GetGalleriesUseCase;
import glitched.adlips.application.community.GetSubscribedGalleriesUseCase;
import glitched.adlips.application.community.port.out.GalleryQueryItem;
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

@WebMvcTest(GalleryController.class)
@Import(SecurityConfig.class)
class GalleryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean GetGalleriesUseCase getGalleriesUseCase;
    @MockitoBean GetSubscribedGalleriesUseCase getSubscribedGalleriesUseCase;
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
    void 갤러리_목록은_비로그인으로도_조회할_수_있다() throws Exception {
        when(getGalleriesUseCase.execute()).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/v1/galleries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("갤러리 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data[0].galleryId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("기타"))
                .andExpect(jsonPath("$.data[0].description").value("기타 연주와 작곡을 다루는 갤러리입니다."))
                .andExpect(jsonPath("$.data[0].subscriberCount").value(128))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-06-22T12:00:00"));
    }

    @Test
    void 갤러리가_없으면_빈_배열을_반환한다() throws Exception {
        when(getGalleriesUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/galleries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void 가입_갤러리_목록은_인증이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/galleries/subscribed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 가입_갤러리_목록은_인증된_사용자의_목록을_반환한다() throws Exception {
        when(getSubscribedGalleriesUseCase.execute(1L)).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/v1/galleries/subscribed")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("가입한 갤러리 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data[0].galleryId").value(1))
                .andExpect(jsonPath("$.data[0].subscriberCount").value(128));
    }

    private GalleryQueryItem sampleItem() {
        return new GalleryQueryItem(
                1L, "기타", "기타 연주와 작곡을 다루는 갤러리입니다.", 128L, LocalDateTime.of(2026, 6, 22, 12, 0));
    }
}
