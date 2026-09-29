package glitched.adlips.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.GoogleLoginRequest;
import glitched.adlips.adapter.in.web.dto.LoginResponse;
import glitched.adlips.application.auth.AuthResult;
import glitched.adlips.application.auth.GoogleLoginUseCase;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "Google 로그인 API")
@RestController
@ConditionalOnProperty(name = "app.legacy-auth.enabled", havingValue = "true")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final GoogleLoginUseCase loginUseCase;

    public AuthController(GoogleLoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequest request
    ) {
        AuthResult result = loginUseCase.login(request.idToken());
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", LoginResponse.from(result)));
    }
}
