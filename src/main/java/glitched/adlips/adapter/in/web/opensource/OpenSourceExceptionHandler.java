package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.application.opensource.OpenSourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = OpenSourceSaveController.class)
public class OpenSourceExceptionHandler {
    @ExceptionHandler(OpenSourceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(OpenSourceException exception) {
        int status = switch (exception.getCode()) {
            case INVALID_REQUEST -> 400;
            case OPEN_SOURCE_NOT_FOUND -> 404;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(exception.getCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> invalidId(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "ID는 양의 정수여야 합니다."));
    }
}
