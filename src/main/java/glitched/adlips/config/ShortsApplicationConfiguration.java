package glitched.adlips.config;

import glitched.adlips.adapter.out.transaction.SpringTransactionRunner;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.port.out.ProjectMemberJoinPort;
import glitched.adlips.application.project.usecase.ProjectMemberJoinUseCase;
import glitched.adlips.application.shorts.GetShortsComposersUseCase;
import glitched.adlips.application.shorts.GetShortsCompositionDetailUseCase;
import glitched.adlips.application.shorts.GetShortsShareUseCase;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.DeleteShortCommentUseCase;
import glitched.adlips.application.shorts.GetShortCommentsUseCase;
import glitched.adlips.application.shorts.ShortsCompositionEntryUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentReportUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
import glitched.adlips.application.shorts.SubmitShortReportUseCase;
import glitched.adlips.application.shorts.UpdateShortCommentUseCase;
import glitched.adlips.application.shorts.ToggleShortBookmarkUseCase;
import glitched.adlips.application.shorts.ToggleShortCommentLikeUseCase;
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
import glitched.adlips.application.shorts.port.out.ShortReportPort;
import glitched.adlips.application.shorts.port.out.UserBanQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionDetailQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
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
    GetShortsUseCase getShortsUseCase(ShortsQueryPort shortsQueryPort) {
        return new GetShortsUseCase(shortsQueryPort);
    }

    @Bean
    ToggleShortLikeUseCase toggleShortLikeUseCase(
            ShortReactionPort shortReactionPort,
            ShortReactionCountCachePort shortReactionCountCachePort,
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortLikeUseCase(
                shortReactionPort, shortReactionCountCachePort, transactionRunner);
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
    GetShortsComposersUseCase getShortsComposersUseCase(
            ShortsComposersQueryPort shortsComposersQueryPort
    ) {
        return new GetShortsComposersUseCase(shortsComposersQueryPort);
    }

    @Bean
    SubmitShortReportUseCase submitShortReportUseCase(ShortReportPort shortReportPort) {
        return new SubmitShortReportUseCase(shortReportPort);
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
    ) {
        return new SubmitShortCommentUseCase(
                shortCommentPort, userBanQueryPort, transactionRunner, clock);
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
            TransactionRunner transactionRunner
    ) {
        return new ToggleShortCommentLikeUseCase(shortCommentReactionPort, transactionRunner);
    }

    @Bean
    SubmitShortCommentReportUseCase submitShortCommentReportUseCase(
            ShortCommentPort shortCommentPort,
            ShortCommentReportPort shortCommentReportPort
    ) {
        return new SubmitShortCommentReportUseCase(shortCommentPort, shortCommentReportPort);
    }
}
