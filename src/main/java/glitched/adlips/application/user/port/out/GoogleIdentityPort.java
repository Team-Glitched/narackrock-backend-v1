package glitched.adlips.application.user.port.out;

import glitched.adlips.application.user.GoogleIdentity;

public interface GoogleIdentityPort {
    GoogleIdentity verify(String idToken);
}
