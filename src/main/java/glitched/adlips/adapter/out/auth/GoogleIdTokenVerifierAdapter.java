package glitched.adlips.adapter.out.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import glitched.adlips.application.user.GoogleIdentity;
import glitched.adlips.application.user.UserApplicationException;
import glitched.adlips.application.user.UserErrorCode;
import glitched.adlips.application.user.port.out.GoogleIdentityPort;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifierAdapter implements GoogleIdentityPort {
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(@Value("${app.auth.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance()
        ).setAudience(List.of(clientId)).build();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw authenticationFailed(null);
        }
        try {
            GoogleIdToken verifiedToken = verifier.verify(idToken);
            if (verifiedToken == null) {
                throw authenticationFailed(null);
            }
            GoogleIdToken.Payload payload = verifiedToken.getPayload();
            return new GoogleIdentity(
                    payload.getSubject(),
                    payload.getEmail(),
                    Boolean.TRUE.equals(payload.getEmailVerified())
            );
        } catch (GeneralSecurityException | IOException exception) {
            throw authenticationFailed(exception);
        }
    }

    private UserApplicationException authenticationFailed(Throwable cause) {
        return new UserApplicationException(
                UserErrorCode.AUTH_FAILED_GOOGLE,
                "계정 인증을 실패했습니다.",
                cause
        );
    }
}
