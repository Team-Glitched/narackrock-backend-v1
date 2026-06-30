package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiErrorResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {
    @ExceptionHandler(UserApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handle(UserApplicationException exception) {
        Object data = exception.getErrorCode() == UserErrorCode.SIGNUP_REQUIRED
                ? Map.of("signupRequired", true)
                : null;
        ApiErrorResponse body = new ApiErrorResponse(
                false,
                exception.getErrorCode().name(),
                exception.getMessage(),
                data
        );
        return ResponseEntity.status(statusOf(exception.getErrorCode())).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                UserErrorCode.VALIDATION_ERROR.name(),
                "요청 본문을 확인해 주세요."
        ));
    }

    private HttpStatus statusOf(UserErrorCode errorCode) {
        return switch (errorCode) {
            case AUTH_FAILED_GOOGLE, INVALID_REFRESH_TOKEN, UNAUTHORIZED_ACCESS -> HttpStatus.UNAUTHORIZED;
            case SIGNUP_REQUIRED, USER_NOT_FOUND, PROFILE_NOT_FOUND, FOLLOW_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PRIVATE_PROFILE -> HttpStatus.FORBIDDEN;
            case DUPLICATE_NICKNAME, ALREADY_REGISTERED, ALREADY_FOLLOWING -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
