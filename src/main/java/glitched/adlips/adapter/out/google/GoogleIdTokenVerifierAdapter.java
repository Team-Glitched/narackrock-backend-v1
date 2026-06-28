package glitched.adlips.adapter.out.google;

import glitched.adlips.application.auth.GoogleUserInfo;
import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final RestClient restClient;
    private final String googleClientId;

    public GoogleIdTokenVerifierAdapter(
            RestClient restClient,
            @Value("${google.client-id}") String googleClientId
    ) {
        this.restClient = restClient;
        this.googleClientId = googleClientId;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        Map<String, String> payload = fetchTokenInfo(idToken);
        validatePayload(payload);
        return new GoogleUserInfo(payload.get("sub"), payload.get("email"), "true".equals(payload.get("email_verified")));
    }

    private Map<String, String> fetchTokenInfo(String idToken) {
        try {
            return restClient.get()
                    .uri(TOKENINFO_URL + "?id_token=" + idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            throw new AuthFailedException();
        }
    }

    private void validatePayload(Map<String, String> payload) {
        if (payload == null) {
            throw new AuthFailedException();
        }
        if (!googleClientId.equals(payload.get("aud"))) {
            throw new AuthFailedException();
        }
        if (!"true".equals(payload.get("email_verified"))) {
            throw new AuthFailedException();
        }
    }
}
