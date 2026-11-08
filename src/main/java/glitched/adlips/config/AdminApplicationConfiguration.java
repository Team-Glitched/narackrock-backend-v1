package glitched.adlips.config;

import glitched.adlips.application.community.ApproveGalleryProposalUseCase;
import glitched.adlips.application.community.GetGalleryProposalsUseCase;
import glitched.adlips.application.community.RejectGalleryProposalUseCase;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportContentModerationPort;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.application.report.port.out.ReportQueryPort;
import glitched.adlips.application.report.usecase.GetReportsUseCase;
import glitched.adlips.application.report.usecase.ResolveReportUseCase;
import glitched.adlips.application.report.usecase.RejectReportUseCase;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.application.user.admin.usecase.UserBanCancelUseCase;
import glitched.adlips.application.user.admin.usecase.UserBanCreateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminApplicationConfiguration {

    @Bean
    GetReportsUseCase getReportsUseCase(
            ReportQueryPort reportQueryPort,
            UserRepositoryPort userRepositoryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetReportsUseCase(
                reportQueryPort, userRepositoryPort, transactionRunner);
    }

    @Bean
    GetGalleryProposalsUseCase getGalleryProposalsUseCase(
            GalleryProposalPort galleryProposalPort,
            UserRepositoryPort userRepositoryPort,
            TransactionRunner transactionRunner
    ) {
        return new GetGalleryProposalsUseCase(galleryProposalPort, userRepositoryPort, transactionRunner);
    }

    @Bean
    ApproveGalleryProposalUseCase approveGalleryProposalUseCase(
            GalleryProposalPort galleryProposalPort,
            GalleryManagementPort galleryManagementPort,
            UserRepositoryPort userRepositoryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new ApproveGalleryProposalUseCase(
                galleryProposalPort, galleryManagementPort, userRepositoryPort, transactionRunner, clock, notificationEvents);
    }

    @Bean
    RejectGalleryProposalUseCase rejectGalleryProposalUseCase(
            GalleryProposalPort galleryProposalPort,
            UserRepositoryPort userRepositoryPort,
            TransactionRunner transactionRunner,
            Clock clock
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new RejectGalleryProposalUseCase(galleryProposalPort, userRepositoryPort, transactionRunner, clock, notificationEvents);
    }

    @Bean
    UserBanCreateUseCase userBanCreateUseCase(
            UserRepositoryPort userRepositoryPort,
            UserBanRepositoryPort userBanRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new UserBanCreateUseCase(
                userRepositoryPort, userBanRepositoryPort, clock, transactionRunner, notificationEvents);
    }

    @Bean
    UserBanCancelUseCase userBanCancelUseCase(
            UserRepositoryPort userRepositoryPort,
            UserBanRepositoryPort userBanRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new UserBanCancelUseCase(
                userRepositoryPort, userBanRepositoryPort, clock, transactionRunner, notificationEvents);
    }

    @Bean
    ResolveReportUseCase resolveReportUseCase(
            ReportResolutionPort reportResolutionPort,
            ReportContentModerationPort reportContentModerationPort,
            UserBanRepositoryPort userBanRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new ResolveReportUseCase(reportResolutionPort, reportContentModerationPort, userBanRepositoryPort,
                userRepositoryPort, clock, transactionRunner, notificationEvents);
    }

    @Bean
    RejectReportUseCase rejectReportUseCase(
            ReportResolutionPort reportResolutionPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    , glitched.adlips.application.notification.NotificationEvents notificationEvents) {
        return new RejectReportUseCase(reportResolutionPort, userRepositoryPort, clock, transactionRunner, notificationEvents);
    }

}
