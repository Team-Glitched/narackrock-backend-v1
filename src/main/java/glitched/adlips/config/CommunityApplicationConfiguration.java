package glitched.adlips.config;

import glitched.adlips.application.community.CreatePostCommentUseCase;
import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostCommentUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetGalleriesUseCase;
import glitched.adlips.application.community.GetManageableGalleriesUseCase;
import glitched.adlips.application.community.GetGalleryBansUseCase;
import glitched.adlips.application.community.GetGalleryManagersUseCase;
import glitched.adlips.application.community.GetGalleryModerationLogsUseCase;
import glitched.adlips.application.community.GetMyGalleryProposalsUseCase;
import glitched.adlips.application.community.GetMyGalleryRoleUseCase;
import glitched.adlips.application.community.GetPopularPostsUseCase;
import glitched.adlips.application.community.GetPostCommentsUseCase;
import glitched.adlips.application.community.GetPostShareUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.GetSubscribedGalleriesUseCase;
import glitched.adlips.application.community.GrantGalleryManagerUseCase;
import glitched.adlips.application.community.LiftGalleryBanUseCase;
import glitched.adlips.application.community.ModeratePostCommentDeletionUseCase;
import glitched.adlips.application.community.ModeratePostDeletionUseCase;
import glitched.adlips.application.community.RevokeGalleryManagerUseCase;
import glitched.adlips.application.community.SanctionGalleryUserUseCase;
import glitched.adlips.application.community.SubmitGalleryProposalUseCase;
import glitched.adlips.application.community.SubscribeGalleryUseCase;
import glitched.adlips.application.community.TogglePostBookmarkUseCase;
import glitched.adlips.application.community.TogglePostCommentReactionUseCase;
import glitched.adlips.application.community.TogglePostReactionUseCase;
import glitched.adlips.application.community.GetLikedPostsUseCase;
import glitched.adlips.application.community.port.out.LikedPostQueryPort;
import glitched.adlips.application.community.UnsubscribeGalleryUseCase;
import glitched.adlips.application.community.UpdateGalleryUseCase;
import glitched.adlips.application.community.UpdateGalleryImagesUseCase;
import glitched.adlips.application.community.UpdatePostCommentUseCase;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.community.port.out.GalleryQueryPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.community.port.out.PostBookmarkPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.PostCommentQueryPort;
import glitched.adlips.application.community.port.out.PostCommentReactionPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.PostPopularityRankingPort;
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
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunityApplicationConfiguration {
    @Bean
    GetLikedPostsUseCase getLikedPostsUseCase(LikedPostQueryPort likedPosts, TransactionRunner transactionRunner) {
        return new GetLikedPostsUseCase(likedPosts, transactionRunner);
    }

    @Bean
    SubmitReportUseCase submitReportUseCase(
            ReportTargetQueryPort targetQueryPort,
            ReportSubmissionPort submissionPort,
            ReportUserRestrictionPort restrictionPort,
            Clock clock,
            TransactionRunner transactionRunner
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new SubmitReportUseCase(
                targetQueryPort,
                submissionPort,
                restrictionPort,
                clock,
                transactionRunner
        , notificationEvents);
    }

    @Bean
    SubmitGalleryProposalUseCase submitGalleryProposalUseCase(
            GalleryProposalPort galleryProposalPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new SubmitGalleryProposalUseCase(
                galleryProposalPort, userBanQueryPort, transactionRunner, clock, notificationEvents);
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
    UpdateGalleryImagesUseCase updateGalleryImagesUseCase(
            GalleryAuthorityPort authority, GalleryManagementPort management, GalleryModerationLogPort logs,
            MediaFileRepositoryPort media, TransactionRunner transactions, Clock clock) {
        return new UpdateGalleryImagesUseCase(authority, management, logs, media, transactions, clock);
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
                galleryAuthorityPort,
                galleryManagementPort,
                galleryModerationLogPort,
                transactionRunner,
                clock);
    }

    @Bean
    GetGalleryModerationLogsUseCase getGalleryModerationLogsUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new GetGalleryModerationLogsUseCase(
                galleryAuthorityPort,
                galleryModerationLogPort,
                transactionRunner,
                clock);
    }

    @Bean
    SanctionGalleryUserUseCase sanctionGalleryUserUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryManagementPort galleryManagementPort,
            GalleryBanPort galleryBanPort,
            GallerySubscriptionPort gallerySubscriptionPort,
            UserRepositoryPort userRepositoryPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new SanctionGalleryUserUseCase(
                galleryAuthorityPort,
                galleryManagementPort,
                galleryBanPort,
                gallerySubscriptionPort,
                userRepositoryPort,
                galleryModerationLogPort,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    LiftGalleryBanUseCase liftGalleryBanUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryManagementPort galleryManagementPort,
            GalleryBanPort galleryBanPort,
            UserRepositoryPort userRepositoryPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new LiftGalleryBanUseCase(
                galleryAuthorityPort,
                galleryManagementPort,
                galleryBanPort,
                userRepositoryPort,
                galleryModerationLogPort,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    GetGalleryManagersUseCase getGalleryManagersUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryManagementPort galleryManagementPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new GetGalleryManagersUseCase(
                galleryAuthorityPort, galleryManagementPort, transactionRunner, clock);
    }

    @Bean
    GrantGalleryManagerUseCase grantGalleryManagerUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryManagementPort galleryManagementPort,
            GalleryBanPort galleryBanPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new GrantGalleryManagerUseCase(
                galleryAuthorityPort,
                galleryManagementPort,
                galleryBanPort,
                galleryModerationLogPort,
                transactionRunner,
                clock);
    }

    @Bean
    RevokeGalleryManagerUseCase revokeGalleryManagerUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryManagementPort galleryManagementPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new RevokeGalleryManagerUseCase(
                galleryAuthorityPort,
                galleryManagementPort,
                galleryModerationLogPort,
                transactionRunner,
                clock);
    }

    @Bean
    GetGalleryBansUseCase getGalleryBansUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            GalleryBanPort galleryBanPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new GetGalleryBansUseCase(
                galleryAuthorityPort, galleryBanPort, transactionRunner, clock);
    }

    @Bean
    ModeratePostDeletionUseCase moderatePostDeletionUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            PostPort postPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new ModeratePostDeletionUseCase(
                galleryAuthorityPort,
                postPort,
                galleryModerationLogPort,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    ModeratePostCommentDeletionUseCase moderatePostCommentDeletionUseCase(
            GalleryAuthorityPort galleryAuthorityPort,
            PostPort postPort,
            PostCommentPort postCommentPort,
            GalleryModerationLogPort galleryModerationLogPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new ModeratePostCommentDeletionUseCase(
                galleryAuthorityPort,
                postPort,
                postCommentPort,
                galleryModerationLogPort,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    GetPostCommentsUseCase getPostCommentsUseCase(
            PostCommentQueryPort port,
            TransactionRunner transactionRunner
    ) {
        return new GetPostCommentsUseCase(port, transactionRunner);
    }

    @Bean
    CreatePostUseCase createPostUseCase(
            PostPort postPort,
            GallerySubscriptionPort subscriptions,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new CreatePostUseCase(
                postPort,
                subscriptions,
                userBanQueryPort,
                galleryBanQueryPort,
                mediaFileRepository,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    GetManageableGalleriesUseCase getManageableGalleriesUseCase(
            GalleryQueryPort galleries, UserBanQueryPort bans, TransactionRunner transactions, Clock clock
    ) {
        return new GetManageableGalleriesUseCase(galleries, bans, transactions, clock);
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
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new SubscribeGalleryUseCase(
                gallerySubscriptionPort,
                galleryBanQueryPort,
                transactionRunner,
                clock);
    }

    @Bean
    UnsubscribeGalleryUseCase unsubscribeGalleryUseCase(
            GallerySubscriptionPort gallerySubscriptionPort,
            TransactionRunner transactionRunner
    ) {
        return new UnsubscribeGalleryUseCase(gallerySubscriptionPort, transactionRunner);
    }

    @Bean
    GetPostUseCase getPostUseCase(
            PostQueryPort postQueryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetPostUseCase(postQueryPort, transactionRunner);
    }

    @Bean
    GetPostsUseCase getPostsUseCase(
            PostQueryPort postQueryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetPostsUseCase(postQueryPort, transactionRunner);
    }

    @Bean
    GetPopularPostsUseCase getPopularPostsUseCase(
            PostQueryPort postQueryPort,
            PostPopularityRankingPort ranking,
            TransactionRunner transactionRunner
    ) {
        return new GetPopularPostsUseCase(postQueryPort, ranking, transactionRunner);
    }

    @Bean
    GetPostShareUseCase getPostShareUseCase(
            PostSharePort postSharePort,
            PostShareLinkPort postShareLinkPort
    ) {
        return new GetPostShareUseCase(postSharePort, postShareLinkPort);
    }

    @Bean
    UpdatePostUseCase updatePostUseCase(
            PostPort postPort,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new UpdatePostUseCase(
                postPort,
                userBanQueryPort,
                galleryBanQueryPort,
                transactionRunner,
                clock);
    }

    @Bean
    DeletePostUseCase deletePostUseCase(
            PostPort postPort,
            PostPopularityRankingPort ranking,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new DeletePostUseCase(
                postPort, ranking, userBanQueryPort, transactionRunner, clock);
    }

    @Bean
    TogglePostReactionUseCase togglePostReactionUseCase(
            PostReactionPort postReactionPort,
            PostPopularityRankingPort ranking,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new TogglePostReactionUseCase(
                postReactionPort,
                ranking,
                userBanQueryPort,
                galleryBanQueryPort,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    TogglePostCommentReactionUseCase togglePostCommentReactionUseCase(
            PostCommentReactionPort postCommentReactionPort,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new TogglePostCommentReactionUseCase(
                postCommentReactionPort,
                userBanQueryPort,
                galleryBanQueryPort,
                transactionRunner,
                clock, notificationEvents);
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
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new CreatePostCommentUseCase(
                postCommentPort,
                userBanQueryPort,
                galleryBanQueryPort,
                transactionRunner,
                clock, notificationEvents);
    }

    @Bean
    UpdatePostCommentUseCase updatePostCommentUseCase(
            PostCommentPort postCommentPort,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new UpdatePostCommentUseCase(
                postCommentPort,
                userBanQueryPort,
                galleryBanQueryPort,
                transactionRunner,
                clock);
    }

    @Bean
    DeletePostCommentUseCase deletePostCommentUseCase(
            PostCommentPort postCommentPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new DeletePostCommentUseCase(
                postCommentPort, userBanQueryPort, transactionRunner, clock);
    }
}