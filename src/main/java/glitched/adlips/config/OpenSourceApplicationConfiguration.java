package glitched.adlips.config;

import glitched.adlips.application.opensource.OpenSourceSavePort;
import glitched.adlips.application.opensource.OpenSourceQueryPort;
import glitched.adlips.application.opensource.OpenSourceQueryUseCase;
import glitched.adlips.application.opensource.OpenSourcePublicationPort;
import glitched.adlips.application.opensource.OpenSourcePublicationUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import java.time.Clock;
import glitched.adlips.application.opensource.OpenSourceSaveUseCase;
import glitched.adlips.application.port.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSourceApplicationConfiguration {
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
