package glitched.adlips.adapter.in.web.user;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.profile.dto.request.UserProfileGetRequest;
import glitched.adlips.application.user.profile.dto.request.UserProfileImageUpdateRequest;
import glitched.adlips.application.user.profile.dto.request.UserProfileShareRequest;
import glitched.adlips.application.user.profile.dto.request.UserProfileUpdateRequest;
import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;
import glitched.adlips.application.user.profile.dto.response.UserProfileImageUpdateResponse;
import glitched.adlips.application.user.profile.dto.response.UserProfileShareResponse;
import glitched.adlips.application.user.profile.dto.response.UserProfileUpdateResponse;
import glitched.adlips.application.user.profile.usecase.ProfileGetUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileImageUpdateUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileShareUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileUpdateUseCase;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "프로필", description = "사용자 프로필 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users")
public class ProfileController {
    private final ProfileGetUseCase profileGetUseCase;
    private final ProfileUpdateUseCase profileUpdateUseCase;
    private final ProfileImageUpdateUseCase profileImageUpdateUseCase;
    private final ProfileShareUseCase profileShareUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProfileController(
            ProfileGetUseCase profileGetUseCase,
            ProfileUpdateUseCase profileUpdateUseCase,
            ProfileImageUpdateUseCase profileImageUpdateUseCase,
            ProfileShareUseCase profileShareUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.profileGetUseCase = profileGetUseCase;
        this.profileUpdateUseCase = profileUpdateUseCase;
        this.profileImageUpdateUseCase = profileImageUpdateUseCase;
        this.profileShareUseCase = profileShareUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/profiles/{userId}")
    public ApiResponse<UserProfileGetResponse> getProfile(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "프로필 조회가 완료되었습니다.",
                profileGetUseCase.execute(new UserProfileGetRequest(userId, requesterId))
        );
    }

    @PatchMapping("/me/profile")
    public ApiResponse<UserProfileUpdateResponse> updateProfile(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UserProfileUpdateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        UserProfileUpdateResponse result = profileUpdateUseCase.execute(
                new UserProfileUpdateRequest(
                        userId, request.nickname(), request.primaryInstrument(), request.explanation()
                )
        );
        return ApiResponse.success("프로필 정보가 성공적으로 수정되었습니다.", result);
    }

    @PatchMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileImageUpdateResponse> updateProfileImage(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        UserProfileImageUpdateResponse result = profileImageUpdateUseCase.execute(
                new UserProfileImageUpdateRequest(
                        userId, image.getOriginalFilename(), image.getContentType(), image.getBytes()
                )
        );
        return ApiResponse.success("프로필 이미지가 성공적으로 수정되었습니다.", result);
    }

    @GetMapping("/profiles/{userId}/share")
    public ApiResponse<UserProfileShareResponse> shareProfile(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "프로필 공유 링크 조회가 완료되었습니다.",
                profileShareUseCase.execute(new UserProfileShareRequest(userId))
        );
    }
}
