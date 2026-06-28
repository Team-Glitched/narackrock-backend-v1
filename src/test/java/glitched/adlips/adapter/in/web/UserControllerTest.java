package glitched.adlips.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.application.auth.AuthResult;
import glitched.adlips.application.auth.SignUpWithGoogleUseCase;
import glitched.adlips.application.exception.AlreadyRegisteredException;
import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.exception.DuplicateNicknameException;
import glitched.adlips.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignUpWithGoogleUseCase signUpUseCase;

    @Test
    void signUpSuccessfully() throws Exception {
        given(signUpUseCase.signUp("valid-token", "guitar_moon"))
                .willReturn(new AuthResult("jwt-token", "Bearer", 1L, "guitar_moon", null));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\", \"nickname\": \"guitar_moon\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.nickname").value("guitar_moon"));
    }

    @Test
    void returnBadRequestWhenNicknameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\", \"nickname\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("닉네임이 입력되지 않았습니다."));
    }

    @Test
    void returnBadRequestWhenNicknameNull() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void returnConflictWhenNicknameDuplicate() throws Exception {
        given(signUpUseCase.signUp(anyString(), anyString())).willThrow(new DuplicateNicknameException());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\", \"nickname\": \"taken\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"))
                .andExpect(jsonPath("$.message").value("이미 사용중인 닉네임입니다."));
    }

    @Test
    void returnConflictWhenGoogleAccountAlreadyRegistered() throws Exception {
        given(signUpUseCase.signUp(anyString(), anyString())).willThrow(new AlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"already-registered\", \"nickname\": \"guitar_moon\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ALREADY_REGISTERED"));
    }

    @Test
    void returnUnauthorizedWhenGoogleTokenInvalid() throws Exception {
        given(signUpUseCase.signUp(anyString(), anyString())).willThrow(new AuthFailedException());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"invalid-token\", \"nickname\": \"guitar_moon\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_FAILED_GOOGLE"));
    }
}
