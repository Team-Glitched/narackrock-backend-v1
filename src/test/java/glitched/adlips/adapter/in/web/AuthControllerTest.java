package glitched.adlips.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.application.auth.AuthResult;
import glitched.adlips.application.auth.GoogleLoginUseCase;
import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.exception.SignupRequiredException;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleLoginUseCase loginUseCase;

    @Test
    void loginSuccessfully() throws Exception {
        given(loginUseCase.login("valid-token"))
                .willReturn(new AuthResult("jwt-token", "Bearer", 1L, "guitar_moon", null));

        mockMvc.perform(post("/api/v1/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.userId").value(1))
                .andExpect(jsonPath("$.data.user.nickname").value("guitar_moon"));
    }

    @Test
    void returnUnauthorizedWhenGoogleTokenInvalid() throws Exception {
        given(loginUseCase.login(anyString())).willThrow(new AuthFailedException());

        mockMvc.perform(post("/api/v1/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_FAILED_GOOGLE"))
                .andExpect(jsonPath("$.message").value("계정 인증을 실패했습니다."));
    }

    @Test
    void returnNotFoundWithSignupFlagWhenUserNotRegistered() throws Exception {
        given(loginUseCase.login(anyString())).willThrow(new SignupRequiredException());

        mockMvc.perform(post("/api/v1/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-but-unregistered\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.data.signupRequired").value(true));
    }

    @Test
    void returnBadRequestWhenIdTokenBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
