package glitched.adlips.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import glitched.adlips.application.auth.AuthResult;

public record LoginResponse(String accessToken, String tokenType, UserData user) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserData(Long userId, String nickname, String profileImageUrl) {}

    public static LoginResponse from(AuthResult result) {
        return new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                new UserData(result.userId(), result.nickname(), result.profileImageUrl())
        );
    }
}
