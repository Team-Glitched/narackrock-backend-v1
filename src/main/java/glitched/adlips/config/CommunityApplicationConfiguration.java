package glitched.adlips.config;

import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunityApplicationConfiguration {

    @Bean
    CreatePostUseCase createPostUseCase(
            PostPort postPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new CreatePostUseCase(postPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    GetPostUseCase getPostUseCase(PostQueryPort postQueryPort, TransactionRunner transactionRunner) {
        return new GetPostUseCase(postQueryPort, transactionRunner);
    }

    @Bean
    GetPostsUseCase getPostsUseCase(PostQueryPort postQueryPort, TransactionRunner transactionRunner) {
        return new GetPostsUseCase(postQueryPort, transactionRunner);
    }

    @Bean
    UpdatePostUseCase updatePostUseCase(
            PostPort postPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new UpdatePostUseCase(postPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    DeletePostUseCase deletePostUseCase(
            PostPort postPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new DeletePostUseCase(postPort, userBanQueryPort, transactionRunner, clock);
    }
}
