package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.account.dto.request.GoogleLoginRequest;
import glitched.adlips.application.user.account.dto.request.TokenRefreshRequest;
import glitched.adlips.application.user.account.dto.request.UserSignupRequest;
import glitched.adlips.application.user.account.dto.request.UserWithdrawRequest;
import glitched.adlips.application.user.account.dto.response.GoogleLoginResponse;
import glitched.adlips.application.user.account.dto.response.TokenRefreshResponse;
import glitched.adlips.application.user.account.dto.response.UserSignupResponse;
import glitched.adlips.application.user.account.usecase.GoogleLoginUseCase;
import glitched.adlips.application.user.account.usecase.TokenRefreshUseCase;
import glitched.adlips.application.user.account.usecase.UserSignupUseCase;
import glitched.adlips.application.user.account.usecase.UserWithdrawUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AccountController {
    private final UserSignupUseCase userSignupUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final TokenRefreshUseCase tokenRefreshUseCase;
    private final UserWithdrawUseCase userWithdrawUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AccountController(
            UserSignupUseCase userSignupUseCase,
            GoogleLoginUseCase googleLoginUseCase,
            TokenRefreshUseCase tokenRefreshUseCase,
            UserWithdrawUseCase userWithdrawUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.userSignupUseCase = userSignupUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.tokenRefreshUseCase = tokenRefreshUseCase;
        this.userWithdrawUseCase = userWithdrawUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserSignupResponse>> signup(@RequestBody UserSignupRequest request) {
        UserSignupResponse result = userSignupUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", result));
    }

    @PostMapping("/auth/login/google")
    public ApiResponse<GoogleLoginResponse> login(@RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(
                "로그인에 성공했습니다.",
                googleLoginUseCase.execute(request)
        );
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest request) {
        return ApiResponse.success("토큰이 재발급되었습니다.", tokenRefreshUseCase.execute(request));
    }

    @DeleteMapping("/users/me")
    public ApiResponse<Void> withdraw(@RequestHeader("Authorization") String authorization) {
        userWithdrawUseCase.execute(new UserWithdrawRequest(
                authenticatedUserResolver.requireUserId(authorization)
        ));
        return ApiResponse.success("회원 탈퇴가 정상적으로 처리되었습니다.");
    }

}
