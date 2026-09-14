package glitched.adlips.application.user.profile.port.out;

import glitched.adlips.application.user.profile.dto.response.UserProfileGetResponse;

public interface ProfileActivityQueryPort {
    UserProfileGetResponse.Activities findByUserId(Long userId);
}
