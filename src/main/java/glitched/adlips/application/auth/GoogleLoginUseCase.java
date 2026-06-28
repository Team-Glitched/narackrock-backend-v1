package glitched.adlips.application.auth;

import glitched.adlips.application.exception.SignupRequiredException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.TokenIssuer;
import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.UserAuthProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GoogleLoginUseCase {

    private final GoogleTokenVerifier tokenVerifier;
    private final UserAuthProviderRepository authProviderRepository;
    private final ProfileRepository profileRepository;
    private final TokenIssuer tokenIssuer;

    public GoogleLoginUseCase(
            GoogleTokenVerifier tokenVerifier,
            UserAuthProviderRepository authProviderRepository,
            ProfileRepository profileRepository,
            TokenIssuer tokenIssuer
    ) {
        this.tokenVerifier = tokenVerifier;
        this.authProviderRepository = authProviderRepository;
        this.profileRepository = profileRepository;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional(readOnly = true)
    public AuthResult login(String idToken) {
        GoogleUserInfo googleUser = tokenVerifier.verify(idToken);

        UserAuthProvider authProvider = authProviderRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleUser.providerUserId())
                .orElseThrow(SignupRequiredException::new);

        Profile profile = profileRepository
                .findByUserId(authProvider.getUser().getId())
                .orElseThrow(SignupRequiredException::new);

        String token = tokenIssuer.issue(authProvider.getUser().getId());

        return new AuthResult(token, "Bearer", authProvider.getUser().getId(), profile.getNickname(), null);
    }
}
