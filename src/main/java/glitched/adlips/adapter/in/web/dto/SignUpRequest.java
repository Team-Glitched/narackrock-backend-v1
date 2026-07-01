package glitched.adlips.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank(message = "idToken이 입력되지 않았습니다.")
        String idToken,

        @NotBlank(message = "닉네임이 입력되지 않았습니다.")
        String nickname
) {}
