package glitched.adlips.config;

import glitched.adlips.application.user.AccountService;
import glitched.adlips.application.user.port.out.AccessTokenPort;
import glitched.adlips.application.user.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.port.out.TransactionPort;
import glitched.adlips.application.user.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.port.out.UserRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AccountService accountService(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepositoryPort,
            UserAuthProviderRepositoryPort userAuthProviderRepositoryPort,
            ProfileRepositoryPort profileRepositoryPort,
            TransactionPort transactionPort,
            Clock clock
    ) {
        return new AccountService(
                googleIdentityPort,
                accessTokenPort,
                userRepositoryPort,
                userAuthProviderRepositoryPort,
                profileRepositoryPort,
                transactionPort,
                clock
        );
    }
}
