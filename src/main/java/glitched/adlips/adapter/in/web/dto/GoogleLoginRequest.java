package glitched.adlips.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken이 입력되지 않았습니다.")
        String idToken
) {}
