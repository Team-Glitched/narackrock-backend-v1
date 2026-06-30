package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.ProfileImageCommand;
import glitched.adlips.application.user.ProfileImageResult;
import glitched.adlips.application.user.ProfileService;
import glitched.adlips.application.user.ProfileShareResult;
import glitched.adlips.application.user.ProfileUpdateResult;
import glitched.adlips.application.user.ProfileView;
import glitched.adlips.application.user.UpdateProfileCommand;
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
@RequestMapping("/api/v1/users")
public class ProfileController {
    private final ProfileService profileService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProfileController(
            ProfileService profileService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.profileService = profileService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/profiles/{userId}")
    public ApiResponse<ProfileView> getProfile(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long requesterId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "프로필 조회가 완료되었습니다.",
                profileService.getProfile(userId, requesterId)
        );
    }

    @PatchMapping("/me/profile")
    public ApiResponse<ProfileUpdateResult> updateProfile(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UpdateProfileRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ProfileUpdateResult result = profileService.updateProfile(
                userId,
                new UpdateProfileCommand(
                        request.nickname(), request.primaryInstrument(), request.explanation()
                )
        );
        return ApiResponse.success("프로필 정보가 성공적으로 수정되었습니다.", result);
    }

    @PatchMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageResult> updateProfileImage(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        ProfileImageResult result = profileService.updateProfileImage(
                userId,
                new ProfileImageCommand(
                        image.getOriginalFilename(), image.getContentType(), image.getBytes()
                )
        );
        return ApiResponse.success("프로필 이미지가 성공적으로 수정되었습니다.", result);
    }

    @GetMapping("/profiles/{userId}/share")
    public ApiResponse<ProfileShareResult> shareProfile(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorization
    ) {
        authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success(
                "프로필 공유 링크 조회가 완료되었습니다.",
                profileService.shareProfile(userId)
        );
    }

    public record UpdateProfileRequest(
            String nickname,
            String primaryInstrument,
            String explanation
    ) {
    }
}
