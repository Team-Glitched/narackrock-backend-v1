package glitched.adlips.global.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import glitched.adlips.adapter.out.google.GoogleIdTokenVerifierAdapter;
import glitched.adlips.adapter.out.transaction.SpringTransactionRunner;
import glitched.adlips.application.auth.GoogleLoginUseCase;
import glitched.adlips.application.auth.SignUpWithGoogleUseCase;
import glitched.adlips.application.port.GoogleTokenVerifier;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.TokenIssuer;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.application.port.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AuthConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${google.client-id}") String clientId
    ) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("google.client-id must not be blank");
        }
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport.Builder().build(),
                GsonFactory.getDefaultInstance()
        ).setAudience(List.of(clientId)).build();
    }

    @Bean
    public GoogleTokenVerifier googleTokenVerifier(GoogleIdTokenVerifier verifier) {
        return new GoogleIdTokenVerifierAdapter(verifier);
    }

    @Bean
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(transactionManager);
    }

    @Bean
    public GoogleLoginUseCase googleLoginUseCase(
            GoogleTokenVerifier tokenVerifier,
            UserAuthProviderRepository authProviderRepository,
            ProfileRepository profileRepository,
            TokenIssuer tokenIssuer,
            TransactionRunner transactionRunner
    ) {
        return new GoogleLoginUseCase(
                tokenVerifier,
                authProviderRepository,
                profileRepository,
                tokenIssuer,
                transactionRunner
        );
    }

    @Bean
    public SignUpWithGoogleUseCase signUpWithGoogleUseCase(
            GoogleTokenVerifier tokenVerifier,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            UserAuthProviderRepository authProviderRepository,
            TokenIssuer tokenIssuer,
            TransactionRunner transactionRunner
    ) {
        return new SignUpWithGoogleUseCase(
                tokenVerifier,
                userRepository,
                profileRepository,
                authProviderRepository,
                tokenIssuer,
                transactionRunner
        );
    }
}
