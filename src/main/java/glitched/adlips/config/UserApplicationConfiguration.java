package glitched.adlips.config;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.RefreshTokenGeneratorPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.account.port.out.UserRefreshTokenRepositoryPort;
import glitched.adlips.application.user.account.usecase.GoogleLoginUseCase;
import glitched.adlips.application.user.account.usecase.RefreshTokenManager;
import glitched.adlips.application.user.account.usecase.TokenRefreshUseCase;
import glitched.adlips.application.user.account.usecase.UserSignupUseCase;
import glitched.adlips.application.user.account.usecase.UserWithdrawUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.FileStoragePort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileLinkPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.usecase.ProfileGetUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileImageUpdateUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileMuteToggleUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileShareUseCase;
import glitched.adlips.application.user.profile.usecase.ProfileUpdateUseCase;
import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.application.user.relation.usecase.FollowCancelUseCase;
import glitched.adlips.application.user.relation.usecase.FollowCreateUseCase;
import glitched.adlips.application.user.relation.usecase.FollowerGetListUseCase;
import glitched.adlips.application.user.relation.usecase.FollowingGetListUseCase;
import glitched.adlips.application.user.relation.usecase.RecommendedUserGetListUseCase;
import glitched.adlips.application.user.relation.usecase.UserSearchUseCase;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RefreshTokenManager refreshTokenManager(
            UserRefreshTokenRepositoryPort repository,
            RefreshTokenGeneratorPort generator,
            Clock clock,
            @Value("${app.auth.refresh-token-validity-seconds}")
            long validitySeconds,
            TransactionRunner transactionRunner
    ) {
        return new RefreshTokenManager(
                repository,
                generator,
                clock,
                validitySeconds,
                transactionRunner
        );
    }

    @Bean
    GoogleLoginUseCase googleLoginUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            RefreshTokenManager refreshTokenManager,
            TransactionRunner transactionRunner
    ) {
        return new GoogleLoginUseCase(
                googleIdentityPort,
                accessTokenPort,
                userRepository,
                authProviderRepository,
                profileRepository,
                refreshTokenManager,
                transactionRunner
        );
    }

    @Bean
    UserSignupUseCase userSignupUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            RefreshTokenManager refreshTokenManager,
            TransactionRunner transactionRunner
    ) {
        return new UserSignupUseCase(
                googleIdentityPort,
                accessTokenPort,
                userRepository,
                authProviderRepository,
                profileRepository,
                refreshTokenManager,
                transactionRunner
        );
    }

    @Bean
    TokenRefreshUseCase tokenRefreshUseCase(
            RefreshTokenManager refreshTokenManager,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner
    ) {
        return new TokenRefreshUseCase(
                refreshTokenManager,
                accessTokenPort,
                userRepository,
                transactionRunner
        );
    }

    @Bean
    UserWithdrawUseCase userWithdrawUseCase(
            UserRepositoryPort userRepository,
            RefreshTokenManager refreshTokenManager,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new UserWithdrawUseCase(
                userRepository,
                refreshTokenManager,
                clock,
                transactionRunner
        );
    }

    @Bean
    ProfileGetUseCase profileGetUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FollowRepositoryPort followRepository,
            TransactionRunner transactionRunner
    ) {
        return new ProfileGetUseCase(
                userRepository,
                profileRepository,
                mediaFileRepository,
                followRepository,
                transactionRunner
        );
    }

    @Bean
    ProfileUpdateUseCase profileUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new ProfileUpdateUseCase(
                userRepository,
                profileRepository,
                clock,
                transactionRunner
        );
    }

    @Bean
    ProfileImageUpdateUseCase profileImageUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            FileStoragePort fileStorage,
            TransactionRunner transactionRunner
    ) {
        return new ProfileImageUpdateUseCase(
                userRepository,
                profileRepository,
                mediaFileRepository,
                fileStorage,
                transactionRunner
        );
    }

    @Bean
    ProfileShareUseCase profileShareUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            ProfileLinkPort profileLinkPort,
            TransactionRunner transactionRunner
    ) {
        return new ProfileShareUseCase(
                userRepository,
                profileRepository,
                profileLinkPort,
                transactionRunner
        );
    }

    @Bean
    ProfileMuteToggleUseCase profileMuteToggleUseCase(
            ProfileRepositoryPort profileRepository,
            TransactionRunner transactionRunner
    ) {
        return new ProfileMuteToggleUseCase(
                profileRepository,
                transactionRunner
        );
    }

    @Bean
    FollowCreateUseCase followCreateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            FollowRepositoryPort followRepository,
            TransactionRunner transactionRunner
    ) {
        return new FollowCreateUseCase(
                userRepository,
                profileRepository,
                followRepository,
                transactionRunner
        );
    }

    @Bean
    FollowCancelUseCase followCancelUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            FollowRepositoryPort followRepository,
            TransactionRunner transactionRunner
    ) {
        return new FollowCancelUseCase(
                userRepository,
                profileRepository,
                followRepository,
                transactionRunner
        );
    }

    @Bean
    FollowerGetListUseCase followerGetListUseCase(
            UserRepositoryPort userRepository,
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        return new FollowerGetListUseCase(
                userRepository,
                profileQuery,
                followRepository,
                mediaFileRepository,
                transactionRunner
        );
    }

    @Bean
    FollowingGetListUseCase followingGetListUseCase(
            UserRepositoryPort userRepository,
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        return new FollowingGetListUseCase(
                userRepository,
                profileQuery,
                followRepository,
                mediaFileRepository,
                transactionRunner
        );
    }

    @Bean
    RecommendedUserGetListUseCase recommendedUserGetListUseCase(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            CollaborationUserQueryPort collaborationUserQueryPort,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        return new RecommendedUserGetListUseCase(
                profileQuery,
                followRepository,
                collaborationUserQueryPort,
                mediaFileRepository,
                transactionRunner
        );
    }

    @Bean
    UserSearchUseCase userSearchUseCase(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        return new UserSearchUseCase(
                profileQuery,
                followRepository,
                mediaFileRepository,
                transactionRunner
        );
    }
}