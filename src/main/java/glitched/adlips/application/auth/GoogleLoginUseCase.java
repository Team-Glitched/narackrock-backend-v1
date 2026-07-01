package glitched.adlips.application.auth;

import glitched.adlips.application.exception.SignupRequiredException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.TokenIssuer;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.UserAuthProvider;

public class GoogleLoginUseCase {

    private final GoogleTokenVerifier tokenVerifier;
    private final UserAuthProviderRepository authProviderRepository;
    private final ProfileRepository profileRepository;
    private final TokenIssuer tokenIssuer;
    private final TransactionRunner transactionRunner;

    public GoogleLoginUseCase(
            GoogleTokenVerifier tokenVerifier,
            UserAuthProviderRepository authProviderRepository,
            ProfileRepository profileRepository,
            TokenIssuer tokenIssuer,
            TransactionRunner transactionRunner
    ) {
        this.tokenVerifier = tokenVerifier;
        this.authProviderRepository = authProviderRepository;
        this.profileRepository = profileRepository;
        this.tokenIssuer = tokenIssuer;
        this.transactionRunner = transactionRunner;
    }

    public AuthResult login(String idToken) {
        GoogleUserInfo googleUser = tokenVerifier.verify(idToken);
        return transactionRunner.readOnly(() -> login(googleUser));
    }

    private AuthResult login(GoogleUserInfo googleUser) {
        UserAuthProvider authProvider = authProviderRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleUser.providerUserId())
                .orElseThrow(SignupRequiredException::new);

        Profile profile = profileRepository
                .findByUserId(authProvider.getUserId())
                .orElseThrow(SignupRequiredException::new);

        String token = tokenIssuer.issue(authProvider.getUserId());

        return new AuthResult(token, "Bearer", authProvider.getUserId(), profile.getNickname(), null);
    }
}
