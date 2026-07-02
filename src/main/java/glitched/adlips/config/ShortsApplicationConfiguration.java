package glitched.adlips.config;

import glitched.adlips.adapter.out.transaction.SpringTransactionRunner;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.ToggleShortBookmarkUseCase;
import glitched.adlips.application.shorts.ToggleShortDislikeUseCase;
import glitched.adlips.application.shorts.ToggleShortLikeUseCase;
import glitched.adlips.application.shorts.port.out.ShortBookmarkPort;
import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ShortsApplicationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(transactionManager);
    }

    @Bean
    GetShortsUseCase getShortsUseCase(ShortsQueryPort shortsQueryPort) {
        return new GetShortsUseCase(shortsQueryPort);
    }

    @Bean
    ToggleShortLikeUseCase toggleShortLikeUseCase(
            ShortReactionPort shortReactionPort,
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortLikeUseCase(shortReactionPort, transactionRunner);
    }

    @Bean
    ToggleShortDislikeUseCase toggleShortDislikeUseCase(
            ShortReactionPort shortReactionPort,
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortDislikeUseCase(shortReactionPort, transactionRunner);
    }

    @Bean
    ToggleShortBookmarkUseCase toggleShortBookmarkUseCase(
            ShortBookmarkPort shortBookmarkPort,
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortBookmarkUseCase(shortBookmarkPort, transactionRunner);
    }
}
