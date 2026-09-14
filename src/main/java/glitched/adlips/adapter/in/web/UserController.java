package glitched.adlips.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.LoginResponse;
import glitched.adlips.adapter.in.web.dto.SignUpRequest;
import glitched.adlips.application.auth.AuthResult;
import glitched.adlips.application.auth.SignUpWithGoogleUseCase;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "레거시 사용자", description = "레거시 회원가입 API")
@RestController
@ConditionalOnProperty(name = "app.legacy-auth.enabled", havingValue = "true")
@RequestMapping("/api/v1/users")
public class UserController {

    private final SignUpWithGoogleUseCase signUpUseCase;

    public UserController(SignUpWithGoogleUseCase signUpUseCase) {
        this.signUpUseCase = signUpUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LoginResponse>> signUp(
            @RequestBody @Valid SignUpRequest request
    ) {
        AuthResult result = signUpUseCase.signUp(request.idToken(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", LoginResponse.from(result)));
    }
}
