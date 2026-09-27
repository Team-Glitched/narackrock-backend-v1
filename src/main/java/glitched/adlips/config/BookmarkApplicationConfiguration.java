package glitched.adlips.config;

import glitched.adlips.application.bookmark.GetBookmarkedPostsUseCase;
import glitched.adlips.application.bookmark.GetBookmarkedShortsUseCase;
import glitched.adlips.application.bookmark.port.out.BookmarkQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookmarkApplicationConfiguration {

    @Bean
    GetBookmarkedPostsUseCase getBookmarkedPostsUseCase(
            BookmarkQueryPort bookmarkQueryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetBookmarkedPostsUseCase(bookmarkQueryPort, transactionRunner);
    }

    @Bean
    GetBookmarkedShortsUseCase getBookmarkedShortsUseCase(
            BookmarkQueryPort bookmarkQueryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetBookmarkedShortsUseCase(bookmarkQueryPort, transactionRunner);
    }
}
