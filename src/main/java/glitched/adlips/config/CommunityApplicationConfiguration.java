package glitched.adlips.config;

import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunityApplicationConfiguration {

    @Bean
    CreatePostUseCase createPostUseCase(PostPort postPort, TransactionRunner transactionRunner) {
        return new CreatePostUseCase(postPort, transactionRunner);
    }

    @Bean
    GetPostUseCase getPostUseCase(PostQueryPort postQueryPort) {
        return new GetPostUseCase(postQueryPort);
    }

    @Bean
    GetPostsUseCase getPostsUseCase(PostQueryPort postQueryPort) {
        return new GetPostsUseCase(postQueryPort);
    }

    @Bean
    UpdatePostUseCase updatePostUseCase(PostPort postPort, TransactionRunner transactionRunner) {
        return new UpdatePostUseCase(postPort, transactionRunner);
    }

    @Bean
    DeletePostUseCase deletePostUseCase(PostPort postPort, TransactionRunner transactionRunner, Clock clock) {
        return new DeletePostUseCase(postPort, transactionRunner, clock);
    }
}
