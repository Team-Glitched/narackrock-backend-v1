package glitched.adlips.adapter.out.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import glitched.adlips.application.auth.GoogleUserInfo;
import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new AuthFailedException();
            }
            GoogleIdToken.Payload payload = token.getPayload();
            validatePayload(payload);
            return new GoogleUserInfo(payload.getSubject(), payload.getEmail(), true);
        } catch (GeneralSecurityException | IOException e) {
            throw new AuthFailedException();
        }
    }

    private void validatePayload(GoogleIdToken.Payload payload) {
        if (payload == null || isBlank(payload.getSubject()) || isBlank(payload.getEmail())) {
            throw new AuthFailedException();
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new AuthFailedException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
