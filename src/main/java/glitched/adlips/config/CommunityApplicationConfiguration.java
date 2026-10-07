package glitched.adlips.config;

import glitched.adlips.application.community.CreatePostCommentUseCase;
import glitched.adlips.application.community.DeletePostCommentUseCase;
import glitched.adlips.application.community.UpdatePostCommentUseCase;
import glitched.adlips.application.community.GetPostCommentsUseCase;
import glitched.adlips.application.community.port.out.PostCommentQueryPort;
import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetGalleriesUseCase;
import glitched.adlips.application.community.GetGalleryModerationLogsUseCase;
import glitched.adlips.application.community.GetMyGalleryProposalsUseCase;
import glitched.adlips.application.community.GetMyGalleryRoleUseCase;
import glitched.adlips.application.community.UpdateGalleryUseCase;
import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.SubmitGalleryProposalUseCase;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.community.GetPopularPostsUseCase;
import glitched.adlips.application.community.GetPostShareUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.GetSubscribedGalleriesUseCase;
import glitched.adlips.application.community.SubscribeGalleryUseCase;
import glitched.adlips.application.community.TogglePostBookmarkUseCase;
import glitched.adlips.application.community.TogglePostCommentReactionUseCase;
import glitched.adlips.application.community.TogglePostReactionUseCase;
import glitched.adlips.application.community.UnsubscribeGalleryUseCase;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.community.port.out.PostBookmarkPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.PostCommentReactionPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.community.port.out.PostReactionPort;
import glitched.adlips.application.community.port.out.PostShareLinkPort;
import glitched.adlips.application.community.port.out.PostSharePort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportSubmissionPort;
import glitched.adlips.application.report.port.out.ReportTargetQueryPort;
import glitched.adlips.application.report.port.out.ReportUserRestrictionPort;
import glitched.adlips.application.report.usecase.SubmitReportUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunityApplicationConfiguration {

    @Bean
    SubmitReportUseCase submitReportUseCase(
            ReportTargetQueryPort targetQueryPort,
            ReportSubmissionPort submissionPort,
            ReportUserRestrictionPort restrictionPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new SubmitReportUseCase(
                targetQueryPort,
                submissionPort,
                restrictionPort,
                clock,
                transactionRunner
        );
    }

    @Bean
    SubmitGalleryProposalUseCase submitGalleryProposalUseCase(
            GalleryProposalPort galleryProposalPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new SubmitGalleryProposalUseCase(galleryProposalPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    GetMyGalleryProposalsUseCase getMyGalleryProposalsUseCase(
            GalleryProposalPort galleryProposalPort,
            TransactionRunner transactionRunner
    ) {
        return new GetMyGalleryProposalsUseCase(galleryProposalPort, transactionRunner);
    }

    @Bean
    GetMyGalleryRoleUseCase getMyGalleryRoleUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new GetMyGalleryRoleUseCase(galleryAuthorityPort, transactionRunner, clock);
    }

    @Bean
    UpdateGalleryUseCase updateGalleryUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryManagementPort galleryManagementPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new UpdateGalleryUseCase(
                galleryAuthorityPort, galleryManagementPort, galleryModerationLogPort, transactionRunner, clock);
    }

    @Bean
    GetGalleryModerationLogsUseCase getGalleryModerationLogsUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new GetGalleryModerationLogsUseCase(
                galleryAuthorityPort, galleryModerationLogPort, transactionRunner, clock);
    }

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
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new CreatePostUseCase(
                postPort, userBanQueryPort, mediaFileRepository, transactionRunner, clock);
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
    GetPopularPostsUseCase getPopularPostsUseCase(PostQueryPort postQueryPort, TransactionRunner transactionRunner) {
        return new GetPopularPostsUseCase(postQueryPort, transactionRunner);
    }

    @Bean
    GetPostShareUseCase getPostShareUseCase(PostSharePort postSharePort, PostShareLinkPort postShareLinkPort) {
        return new GetPostShareUseCase(postSharePort, postShareLinkPort);
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
    TogglePostReactionUseCase togglePostReactionUseCase(
            PostReactionPort postReactionPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new TogglePostReactionUseCase(postReactionPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    TogglePostCommentReactionUseCase togglePostCommentReactionUseCase(
            PostCommentReactionPort postCommentReactionPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new TogglePostCommentReactionUseCase(
                postCommentReactionPort, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    TogglePostBookmarkUseCase togglePostBookmarkUseCase(
            PostBookmarkPort postBookmarkPort,
            TransactionRunner transactionRunner
    ) {
        return new TogglePostBookmarkUseCase(postBookmarkPort, transactionRunner);
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
