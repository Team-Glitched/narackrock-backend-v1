package glitched.adlips.adapter.in.web.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.admin.dto.response.UserBanCancelResponse;
import glitched.adlips.application.user.admin.dto.response.UserBanCreateResponse;
import glitched.adlips.application.user.admin.usecase.UserBanCancelUseCase;
import glitched.adlips.application.user.admin.usecase.UserBanCreateUseCase;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.global.config.SecurityConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserBanController.class)
@Import(SecurityConfig.class)
class AdminUserBanControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenIssuerAdapter jwtTokenIssuerAdapter;

    @MockitoBean
    UserBanCreateUseCase userBanCreateUseCase;

    @MockitoBean
    UserBanCancelUseCase userBanCancelUseCase;

    @MockitoBean
    AuthenticatedUserResolver authenticatedUserResolver;

    @MockitoBean
    AccessTokenPort accessTokenPort;

    String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenIssuerAdapter.issue(1L);
        when(accessTokenPort.verify(validToken)).thenReturn(1L);
        when(authenticatedUserResolver.requireUserId("Bearer " + validToken)).thenReturn(1L);
    }

    @Test
    void 유저_차단_성공시_200과_결과를_반환한다() throws Exception {
        when(userBanCreateUseCase.execute(any())).thenReturn(new UserBanCreateResponse(
                2L,
                7,
                LocalDateTime.of(2026, 7, 16, 0, 0)
        ));

        mockMvc.perform(post("/api/v1/admin/users/2/bans")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"banDurationDays\":7,\"banReason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("해당 유저가 성공적으로 차단되었습니다."))
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.banDurationDays").value(7))
                .andExpect(jsonPath("$.data.bannedUntil").value("2026-07-16T00:00:00"));
    }

    @Test
    void 관리자_권한이_없으면_403을_반환한다() throws Exception {
        when(userBanCreateUseCase.execute(any())).thenThrow(new UserApplicationException(
                UserErrorCode.FORBIDDEN_ADMIN_ACCESS,
                "해당 기능은 관리자 전용 기능입니다. 접근 권한이 없습니다."
        ));

        mockMvc.perform(post("/api/v1/admin/users/2/bans")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"banDurationDays\":7,\"banReason\":\"spam\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ADMIN_ACCESS"));
    }

    @Test
    void 유저_차단_해제_성공시_200과_결과를_반환한다() throws Exception {
        when(userBanCancelUseCase.execute(any())).thenReturn(new UserBanCancelResponse(2L));

        mockMvc.perform(delete("/api/v1/admin/users/2/bans")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("해당 유저의 차단이 해제되었습니다."))
                .andExpect(jsonPath("$.data.userId").value(2));
    }

    @Test
    void 해제할_차단이_없으면_404를_반환한다() throws Exception {
        when(userBanCancelUseCase.execute(any())).thenThrow(new UserApplicationException(
                UserErrorCode.BAN_NOT_FOUND,
                "해제할 차단 내역이 존재하지 않습니다."
        ));

        mockMvc.perform(delete("/api/v1/admin/users/2/bans")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BAN_NOT_FOUND"));
    }
}
