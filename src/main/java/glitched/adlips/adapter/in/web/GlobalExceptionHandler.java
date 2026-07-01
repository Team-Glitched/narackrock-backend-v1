package glitched.adlips.adapter.in.web;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.SignupRequiredData;
import glitched.adlips.application.exception.AlreadyRegisteredException;
import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.exception.DuplicateNicknameException;
import glitched.adlips.application.exception.SignupRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthFailed(AuthFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("AUTH_FAILED_GOOGLE", "계정 인증을 실패했습니다."));
    }

    @ExceptionHandler(SignupRequiredException.class)
    public ResponseEntity<ApiResponse<SignupRequiredData>> handleSignupRequired(SignupRequiredException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.errorWithData(
                        "SIGNUP_REQUIRED",
                        "가입되지 않은 계정입니다. 추가 정보 입력 페이지로 이동합니다.",
                        new SignupRequiredData(true)
                ));
    }

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateNickname(DuplicateNicknameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_NICKNAME", "이미 사용중인 닉네임입니다."));
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyRegistered(AlreadyRegisteredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("ALREADY_REGISTERED", "이미 가입된 Google 계정입니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }
}
