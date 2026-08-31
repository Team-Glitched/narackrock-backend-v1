package glitched.adlips.config;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
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
    UserBanCreateUseCase userBanCreateUseCase(
            UserRepositoryPort userRepositoryPort,
            UserBanRepositoryPort userBanRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new UserBanCreateUseCase(
                userRepositoryPort, userBanRepositoryPort, clock, transactionRunner);
    }

    @Bean
    UserBanCancelUseCase userBanCancelUseCase(
            UserRepositoryPort userRepositoryPort,
            UserBanRepositoryPort userBanRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new UserBanCancelUseCase(
                userRepositoryPort, userBanRepositoryPort, clock, transactionRunner);
    }

    @Bean
    ResolveReportUseCase resolveReportUseCase(
            ReportResolutionPort reportResolutionPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new ResolveReportUseCase(reportResolutionPort, userRepositoryPort, clock, transactionRunner);
    }

    @Bean
    RejectReportUseCase rejectReportUseCase(
            ReportResolutionPort reportResolutionPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new RejectReportUseCase(reportResolutionPort, userRepositoryPort, clock, transactionRunner);
    }

    @Bean
    UserBanCreateUseCase userBanCreateUseCase(
            UserRepositoryPort userRepositoryPort,
            UserBanRepositoryPort userBanRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new UserBanCreateUseCase(
                userRepositoryPort, userBanRepositoryPort, clock, transactionRunner);
    }

    @Bean
    UserBanCancelUseCase userBanCancelUseCase(
            UserRepositoryPort userRepositoryPort,
            UserBanRepositoryPort userBanRepositoryPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new UserBanCancelUseCase(
                userRepositoryPort, userBanRepositoryPort, clock, transactionRunner);
    }
}
