package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.account.AccountService;
import glitched.adlips.application.user.account.AuthResult;
import glitched.adlips.application.user.account.LoginCommand;
import glitched.adlips.application.user.account.SignupCommand;
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
    private final AccountService accountService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AccountController(
            AccountService accountService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.accountService = accountService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AuthResult>> signup(@RequestBody SignupRequest request) {
        AuthResult result = accountService.signup(new SignupCommand(request.idToken(), request.nickname()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", result));
    }

    @PostMapping("/auth/login/google")
    public ApiResponse<AuthResult> login(@RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(
                "로그인에 성공했습니다.",
                accountService.login(new LoginCommand(request.idToken()))
        );
    }

    @DeleteMapping("/users/me")
    public ApiResponse<Void> withdraw(@RequestHeader("Authorization") String authorization) {
        accountService.withdraw(authenticatedUserResolver.requireUserId(authorization));
        return ApiResponse.success("회원 탈퇴가 정상적으로 처리되었습니다.");
    }

    public record SignupRequest(String idToken, String nickname) {
    }

    public record GoogleLoginRequest(String idToken) {
    }
}
