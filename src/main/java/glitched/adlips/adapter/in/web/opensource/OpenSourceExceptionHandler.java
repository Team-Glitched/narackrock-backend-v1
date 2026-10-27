package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.application.opensource.OpenSourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {OpenSourceSaveController.class, OpenSourcePublicationController.class, OpenSourceQueryController.class})
public class OpenSourceExceptionHandler {
    @ExceptionHandler(OpenSourceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(OpenSourceException exception) {
        int status = switch (exception.getCode()) {
            case INVALID_REQUEST -> 400;
            case OPEN_SOURCE_NOT_FOUND, MEDIA_FILE_NOT_FOUND -> 404;
            case MEDIA_FILE_NOT_READY -> 409;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(exception.getCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> invalidBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", "JSON 요청 본문이 올바르지 않습니다."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> invalidId(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "ID 또는 조회 조건의 형식이 올바르지 않습니다."));
    }
}
