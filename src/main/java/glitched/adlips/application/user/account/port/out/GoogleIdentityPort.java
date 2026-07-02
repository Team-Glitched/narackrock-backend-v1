package glitched.adlips.application.user.account.port.out;

import glitched.adlips.application.user.account.dto.response.GoogleIdentityResponse;

public interface GoogleIdentityPort {
    GoogleIdentityResponse verify(String idToken);
}
