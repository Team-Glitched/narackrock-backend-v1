package glitched.adlips.config;

import glitched.adlips.application.opensource.OpenSourceSavePort;
import glitched.adlips.application.opensource.OpenSourceInteractionPort;
import glitched.adlips.application.opensource.OpenSourceInteractionUseCase;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.opensource.OpenSourceQueryPort;
import glitched.adlips.application.opensource.OpenSourceQueryUseCase;
import glitched.adlips.application.opensource.OpenSourcePublicationPort;
import glitched.adlips.application.opensource.OpenSourcePublicationUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import java.time.Clock;
import glitched.adlips.application.opensource.OpenSourceMediaUseCase;
import glitched.adlips.application.opensource.OpenSourceMediaPort;
import glitched.adlips.application.opensource.OpenSourceMediaLinkPort;
import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.adapter.out.opensource.SignedOpenSourceMediaLinkAdapter;
import org.springframework.beans.factory.annotation.Value;
import glitched.adlips.application.opensource.OpenSourceSaveUseCase;
import glitched.adlips.application.port.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSourceApplicationConfiguration {
    @Bean
    OpenSourceMediaLinkPort openSourceMediaLinkPort(
            @Value("${app.api.public-base-url}") String apiBaseUrl,
            @Value("${app.opensource.share-base-url:http://localhost:5173/opensource}") String shareBaseUrl,
            @Value("${app.opensource.playback-secret:${app.auth.token-secret}}") String secret,
            @Value("${app.opensource.playback-ttl-seconds:600}") long ttl, Clock clock) {
        return new SignedOpenSourceMediaLinkAdapter(apiBaseUrl, shareBaseUrl, secret, ttl, clock);
    }

    @Bean
    OpenSourceMediaUseCase openSourceMediaUseCase(OpenSourceMediaPort sources, MediaFileRepositoryPort media,
            MediaContentStoragePort storage, OpenSourceMediaLinkPort links, TransactionRunner transactions) {
        return new OpenSourceMediaUseCase(sources, media, storage, links, transactions);
    }

    @Bean
    OpenSourceInteractionUseCase openSourceInteractionUseCase(
            OpenSourceInteractionPort port, TransactionRunner transactions,
            UserBanQueryPort bans, Clock clock, glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new OpenSourceInteractionUseCase(port, transactions, bans, clock, notificationEvents);
    }

    @Bean
    OpenSourceQueryUseCase openSourceQueryUseCase(OpenSourceQueryPort query, TransactionRunner transactions) {
        return new OpenSourceQueryUseCase(query, transactions);
    }

    @Bean
    OpenSourcePublicationUseCase openSourcePublicationUseCase(
            OpenSourcePublicationPort publications,
            MediaFileRepositoryPort media,
            TransactionRunner transactions, Clock clock) {
        return new OpenSourcePublicationUseCase(publications, media, transactions, clock);
    }

    @Bean
    OpenSourceSaveUseCase openSourceSaveUseCase(OpenSourceSavePort port, TransactionRunner transactions) {
        return new OpenSourceSaveUseCase(port, transactions);
    }
}
