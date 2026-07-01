package glitched.adlips.application.port;

import glitched.adlips.application.auth.GoogleUserInfo;

public interface GoogleTokenVerifier {
    GoogleUserInfo verify(String idToken);
}
