package glitched.adlips.config;

import glitched.adlips.adapter.out.transaction.SpringTransactionRunner;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectMemberJoinPort;
import glitched.adlips.application.project.usecase.ProjectMemberJoinUseCase;
import glitched.adlips.application.shorts.GetShortsComposersUseCase;
import glitched.adlips.application.shorts.GetShortsParticipantTracksUseCase;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.shorts.GetShortsCompositionDetailUseCase;
import glitched.adlips.application.shorts.GetShortsShareUseCase;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.GetHomeShortsUseCase;
import glitched.adlips.application.shorts.GetRecommendedShortsUseCase;
import glitched.adlips.application.shorts.DeleteShortCommentUseCase;
import glitched.adlips.application.shorts.GetShortCommentsUseCase;
import glitched.adlips.application.shorts.ShortsCompositionEntryUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentReportUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
import glitched.adlips.application.shorts.SubmitShortReportUseCase;
import glitched.adlips.application.shorts.UpdateShortCommentUseCase;
import glitched.adlips.application.shorts.ToggleShortBookmarkUseCase;
import glitched.adlips.application.shorts.ToggleShortCommentLikeUseCase;
import glitched.adlips.application.shorts.ToggleShortCommentDislikeUseCase;
import glitched.adlips.application.shorts.ToggleShortDislikeUseCase;
import glitched.adlips.application.shorts.ToggleShortLikeUseCase;
import glitched.adlips.application.shorts.ToggleShortPlaybackUseCase;
import glitched.adlips.application.shorts.port.out.ShortBookmarkPort;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.application.shorts.port.out.ShortCommentQueryPort;
import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.application.shorts.port.out.ShortCommentReportPort;
import glitched.adlips.application.shorts.port.out.ShortPlaybackPort;
import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.application.shorts.port.out.ShortPopularityRankingPort;
import glitched.adlips.application.shorts.port.out.ShortReportPort;
import glitched.adlips.application.shorts.port.out.UserBanQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionDetailQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;
import glitched.adlips.application.shorts.port.out.ShortRecommendationQueryPort;
import glitched.adlips.application.shorts.port.out.ShortRecommendationSnapshotPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.user.relation.port.out.CollaborationUserQueryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.shorts.port.out.ShortsShareLinkPort;
import glitched.adlips.application.shorts.port.out.ShortsSharePort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import java.time.Clock;
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
    GetShortsUseCase getShortsUseCase(
            ShortsQueryPort shortsQueryPort,
            ShortReactionCountCachePort shortReactionCountCachePort
    ) {
        return new GetShortsUseCase(shortsQueryPort, shortReactionCountCachePort);
    }

    @Bean
    GetHomeShortsUseCase getHomeShortsUseCase(
            ShortsQueryPort shortsQueryPort,
            ShortReactionCountCachePort shortReactionCountCachePort,
            ShortPopularityRankingPort shortPopularityRankingPort
    ) {
        return new GetHomeShortsUseCase(
                shortsQueryPort, shortReactionCountCachePort, shortPopularityRankingPort);
    }

    @Bean
    GetRecommendedShortsUseCase getRecommendedShortsUseCase(
            ShortRecommendationQueryPort candidates,
            ShortsQueryPort shorts,
            FollowRepositoryPort follows,
            CollaborationUserQueryPort collaborations,
            ShortRecommendationSnapshotPort snapshots
    ) {
        return new GetRecommendedShortsUseCase(candidates, shorts, follows, collaborations, snapshots);
    }

    @Bean
    ToggleShortLikeUseCase toggleShortLikeUseCase(
            ShortReactionPort shortReactionPort,
            ShortReactionCountCachePort shortReactionCountCachePort,
            ShortPopularityRankingPort shortPopularityRankingPort,
            TransactionRunner transactionRunner
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new ToggleShortLikeUseCase(
                shortReactionPort, shortReactionCountCachePort,
                shortPopularityRankingPort, transactionRunner, notificationEvents);
    }

    @Bean
    ToggleShortDislikeUseCase toggleShortDislikeUseCase(
            ShortReactionPort shortReactionPort,
            ShortReactionCountCachePort shortReactionCountCachePort,
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortDislikeUseCase(
                shortReactionPort, shortReactionCountCachePort, transactionRunner);
    }

    @Bean
    ToggleShortBookmarkUseCase toggleShortBookmarkUseCase(
            ShortBookmarkPort shortBookmarkPort,
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortBookmarkUseCase(shortBookmarkPort, transactionRunner);
    }

    @Bean
    ToggleShortPlaybackUseCase toggleShortPlaybackUseCase(
            ShortPlaybackPort shortPlaybackPort,
            Clock clock
    ) {
        return new ToggleShortPlaybackUseCase(shortPlaybackPort, clock);
    }

    @Bean
    ShortsCompositionEntryUseCase shortsCompositionEntryUseCase(
            ShortsCompositionQueryPort shortsCompositionQueryPort,
            ProjectMemberJoinUseCase projectMemberJoinUseCase,
            TransactionRunner transactionRunner
    ) {
        return new ShortsCompositionEntryUseCase(
                shortsCompositionQueryPort, projectMemberJoinUseCase, transactionRunner);
    }

    @Bean
    ProjectMemberJoinUseCase projectMemberJoinUseCase(
            ProjectMemberJoinPort projectMemberJoinPort,
            UserRepositoryPort userRepositoryPort
    ) {
        return new ProjectMemberJoinUseCase(projectMemberJoinPort, userRepositoryPort);
    }

    @Bean
    GetShortsParticipantTracksUseCase getShortsParticipantTracksUseCase(ShortsCompositionQueryPort shorts,
            ProjectRepositoryPort projects, ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
            MediaFileRepositoryPort media, TransactionRunner transactions) {
        return new GetShortsParticipantTracksUseCase(shorts, projects, tracks, clips, media, transactions);
    }

    @Bean
    GetShortsComposersUseCase getShortsComposersUseCase(
            ShortsComposersQueryPort shortsComposersQueryPort
    ) {
        return new GetShortsComposersUseCase(shortsComposersQueryPort);
    }

    @Bean
    SubmitShortReportUseCase submitShortReportUseCase(ShortReportPort shortReportPort, TransactionRunner transactionRunner, glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new SubmitShortReportUseCase(shortReportPort, transactionRunner, notificationEvents);
    }

    @Bean
    GetShortsCompositionDetailUseCase getShortsCompositionDetailUseCase(
            ShortsCompositionDetailQueryPort shortsCompositionDetailQueryPort,
            ShortsComposersQueryPort shortsComposersQueryPort
    ) {
        return new GetShortsCompositionDetailUseCase(
                shortsCompositionDetailQueryPort, shortsComposersQueryPort);
    }

    @Bean
    GetShortsShareUseCase getShortsShareUseCase(
            ShortsSharePort shortsSharePort,
            ShortsShareLinkPort shortsShareLinkPort
    ) {
        return new GetShortsShareUseCase(shortsSharePort, shortsShareLinkPort);
    }

    @Bean
    SubmitShortCommentUseCase submitShortCommentUseCase(
            ShortCommentPort shortCommentPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new SubmitShortCommentUseCase(
                shortCommentPort, userBanQueryPort, transactionRunner, clock, notificationEvents);
    }

    @Bean
    GetShortCommentsUseCase getShortCommentsUseCase(ShortCommentQueryPort shortCommentQueryPort) {
        return new GetShortCommentsUseCase(shortCommentQueryPort);
    }

    @Bean
    UpdateShortCommentUseCase updateShortCommentUseCase(
            ShortCommentPort shortCommentPort,
            TransactionRunner transactionRunner
    ) {
        return new UpdateShortCommentUseCase(shortCommentPort, transactionRunner);
    }

    @Bean
    DeleteShortCommentUseCase deleteShortCommentUseCase(
            ShortCommentPort shortCommentPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new DeleteShortCommentUseCase(shortCommentPort, transactionRunner, clock);
    }

    @Bean
    ToggleShortCommentLikeUseCase toggleShortCommentLikeUseCase(
            ShortCommentReactionPort shortCommentReactionPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock,
            glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new ToggleShortCommentLikeUseCase(
                shortCommentReactionPort, transactionRunner, notificationEvents, userBanQueryPort, clock);
    }

    @Bean
    ToggleShortCommentDislikeUseCase toggleShortCommentDislikeUseCase(
            ShortCommentReactionPort shortCommentReactionPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new ToggleShortCommentDislikeUseCase(
                shortCommentReactionPort, transactionRunner, userBanQueryPort, clock);
    }

    @Bean
    SubmitShortCommentReportUseCase submitShortCommentReportUseCase(
            ShortCommentPort shortCommentPort,
            ShortCommentReportPort shortCommentReportPort
    , TransactionRunner transactionRunner, glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new SubmitShortCommentReportUseCase(shortCommentPort, shortCommentReportPort, transactionRunner, notificationEvents);
    }
}
