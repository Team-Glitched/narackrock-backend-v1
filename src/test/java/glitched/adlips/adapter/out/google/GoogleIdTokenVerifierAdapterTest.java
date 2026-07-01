package glitched.adlips.adapter.out.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import glitched.adlips.application.auth.GoogleUserInfo;
import glitched.adlips.application.exception.AuthFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierAdapterTest {

    @Mock
    private GoogleIdTokenVerifier verifier;

    @Mock
    private GoogleIdToken token;

    @Test
    void returnsVerifiedGoogleUser() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-sub-123")
                .setEmail("user@gmail.com")
                .setEmailVerified(true);
        given(verifier.verify("valid-token")).willReturn(token);
        given(token.getPayload()).willReturn(payload);
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(verifier);

        GoogleUserInfo userInfo = adapter.verify("valid-token");

        assertThat(userInfo.providerUserId()).isEqualTo("google-sub-123");
        assertThat(userInfo.email()).isEqualTo("user@gmail.com");
        assertThat(userInfo.emailVerified()).isTrue();
    }

    @Test
    void rejectsUnverifiedEmail() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-sub-123")
                .setEmail("user@gmail.com")
                .setEmailVerified(false);
        given(verifier.verify("invalid-token")).willReturn(token);
        given(token.getPayload()).willReturn(payload);
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(verifier);

        assertThatThrownBy(() -> adapter.verify("invalid-token"))
                .isInstanceOf(AuthFailedException.class);
    }

    @Test
    void rejectsMissingRequiredClaims() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setEmail("user@gmail.com")
                .setEmailVerified(true);
        given(verifier.verify("invalid-token")).willReturn(token);
        given(token.getPayload()).willReturn(payload);
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(verifier);

        assertThatThrownBy(() -> adapter.verify("invalid-token"))
                .isInstanceOf(AuthFailedException.class);
    }
}
