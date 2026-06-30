package glitched.adlips.application.user.account.port.out;

import glitched.adlips.application.user.account.GoogleIdentity;

public interface GoogleIdentityPort {
    GoogleIdentity verify(String idToken);
}
