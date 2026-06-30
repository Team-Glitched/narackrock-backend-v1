package glitched.adlips.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, String errorCode, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, errorCode, null);
    }

    public static <T> ApiResponse<T> errorWithData(String errorCode, String message, T data) {
        return new ApiResponse<>(false, message, errorCode, data);
    }
}
