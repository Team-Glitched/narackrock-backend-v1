package glitched.adlips.config;

import glitched.adlips.application.community.CreatePostCommentUseCase;
import glitched.adlips.application.community.DeletePostCommentUseCase;
import glitched.adlips.application.community.UpdatePostCommentUseCase;
import glitched.adlips.application.community.GetPostCommentsUseCase;
import glitched.adlips.application.community.port.out.PostCommentQueryPort;
import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetGalleriesUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.GetSubscribedGalleriesUseCase;
import glitched.adlips.application.community.SubscribeGalleryUseCase;
import glitched.adlips.application.community.UnsubscribeGalleryUseCase;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
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
    GetPostCommentsUseCase getPostCommentsUseCase(
            PostCommentQueryPort port,
            TransactionRunner transactionRunner) {
        return new GetPostCommentsUseCase(port, transactionRunner);
    }

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
    GetGalleriesUseCase getGalleriesUseCase(
            GalleryQueryPort galleryQueryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetGalleriesUseCase(galleryQueryPort, transactionRunner);
    }

    @Bean
    GetSubscribedGalleriesUseCase getSubscribedGalleriesUseCase(
            GalleryQueryPort galleryQueryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetSubscribedGalleriesUseCase(galleryQueryPort, transactionRunner);
    }

    @Bean
    SubscribeGalleryUseCase subscribeGalleryUseCase(
            GallerySubscriptionPort gallerySubscriptionPort,
            TransactionRunner transactionRunner
    ) {
        return new SubscribeGalleryUseCase(gallerySubscriptionPort, transactionRunner);
    }

    @Bean
    UnsubscribeGalleryUseCase unsubscribeGalleryUseCase(
            GallerySubscriptionPort gallerySubscriptionPort,
            TransactionRunner transactionRunner
    ) {
        return new UnsubscribeGalleryUseCase(gallerySubscriptionPort, transactionRunner);
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

    @Bean
    CreatePostCommentUseCase createPostCommentUseCase(
            PostCommentPort postCommentPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new CreatePostCommentUseCase(postCommentPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    UpdatePostCommentUseCase updatePostCommentUseCase(
            PostCommentPort postCommentPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new UpdatePostCommentUseCase(postCommentPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    DeletePostCommentUseCase deletePostCommentUseCase(
            PostCommentPort postCommentPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new DeletePostCommentUseCase(postCommentPort, userBanQueryPort, transactionRunner, clock);
    }
}
