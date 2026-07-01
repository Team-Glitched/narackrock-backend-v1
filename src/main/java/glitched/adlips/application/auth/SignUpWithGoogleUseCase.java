package glitched.adlips.application.auth;

import glitched.adlips.application.exception.AlreadyRegisteredException;
import glitched.adlips.application.exception.DuplicateNicknameException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.TokenIssuer;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.application.port.UserRepository;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;

public class SignUpWithGoogleUseCase {

    private final GoogleTokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserAuthProviderRepository authProviderRepository;
    private final TokenIssuer tokenIssuer;
    private final TransactionRunner transactionRunner;

    public SignUpWithGoogleUseCase(
            GoogleTokenVerifier tokenVerifier,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            UserAuthProviderRepository authProviderRepository,
            TokenIssuer tokenIssuer,
            TransactionRunner transactionRunner
    ) {
        this.tokenVerifier = tokenVerifier;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.authProviderRepository = authProviderRepository;
        this.tokenIssuer = tokenIssuer;
        this.transactionRunner = transactionRunner;
    }

    public AuthResult signUp(String idToken, String nickname) {
        GoogleUserInfo googleUser = tokenVerifier.verify(idToken);
        return transactionRunner.required(() -> signUp(googleUser, nickname));
    }

    private AuthResult signUp(GoogleUserInfo googleUser, String nickname) {
        if (authProviderRepository.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, googleUser.providerUserId())) {
            throw new AlreadyRegisteredException();
        }

        if (userRepository.findByEmail(googleUser.email()).isPresent()) {
            throw new AlreadyRegisteredException();
        }

        if (profileRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }

        User user = userRepository.save(User.create(googleUser.email()));
        profileRepository.save(Profile.create(user.getId(), nickname));
        authProviderRepository.save(UserAuthProvider.google(
                user.getId(), googleUser.providerUserId(), googleUser.emailVerified()));

        String token = tokenIssuer.issue(user.getId());

        return new AuthResult(token, "Bearer", user.getId(), nickname, null);
    }
}
