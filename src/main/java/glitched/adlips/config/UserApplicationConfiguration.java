package glitched.adlips.config;

import glitched.adlips.application.user.account.AccountService;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.common.port.out.TransactionPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.ProfileService;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileLinkPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.FollowService;
import glitched.adlips.application.user.relation.UserDiscoveryService;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
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

    @Bean
    ProfileService profileService(
            UserRepositoryPort userRepositoryPort,
            ProfileRepositoryPort profileRepositoryPort,
            MediaFileRepositoryPort mediaFileRepositoryPort,
            FollowRepositoryPort followRepositoryPort,
            FileStoragePort fileStoragePort,
            ProfileLinkPort profileLinkPort,
            TransactionPort transactionPort,
            Clock clock
    ) {
        return new ProfileService(
                userRepositoryPort,
                profileRepositoryPort,
                mediaFileRepositoryPort,
                followRepositoryPort,
                fileStoragePort,
                profileLinkPort,
                transactionPort,
                clock
        );
    }

    @Bean
    FollowService followService(
            UserRepositoryPort userRepositoryPort,
            ProfileRepositoryPort profileRepositoryPort,
            ProfileQueryPort profileQueryPort,
            FollowRepositoryPort followRepositoryPort,
            MediaFileRepositoryPort mediaFileRepositoryPort,
            TransactionPort transactionPort
    ) {
        return new FollowService(
                userRepositoryPort,
                profileRepositoryPort,
                profileQueryPort,
                followRepositoryPort,
                mediaFileRepositoryPort,
                transactionPort
        );
    }

    @Bean
    UserDiscoveryService userDiscoveryService(
            ProfileQueryPort profileQueryPort,
            FollowRepositoryPort followRepositoryPort,
            MediaFileRepositoryPort mediaFileRepositoryPort
    ) {
        return new UserDiscoveryService(
                profileQueryPort,
                followRepositoryPort,
                mediaFileRepositoryPort
        );
    }
}
