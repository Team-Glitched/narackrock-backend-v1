package glitched.adlips.config;

import glitched.adlips.adapter.out.persistence.project.ProjectClipJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionItemJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectContributionJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectExportJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.adapter.out.persistence.project.ProjectTrackJpaRepository;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.usecase.AudioClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipSaveUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionGetListUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectDeleteUseCase;
import glitched.adlips.application.project.usecase.ProjectPublishUseCase;
import glitched.adlips.application.project.usecase.TrackCreateUseCase;
import glitched.adlips.application.project.usecase.TrackDeleteUseCase;
import glitched.adlips.application.project.usecase.TrackGetListUseCase;
import glitched.adlips.application.project.usecase.TrackVolumeUpdateUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectApplicationConfiguration {

    @Bean
    ProjectCreateUseCase projectCreateUseCase(
            UserRepositoryPort users,
            MediaFileRepositoryPort mediaFiles,
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            TransactionRunner transactionRunner
    ) {
        return new ProjectCreateUseCase(users, mediaFiles, projects, members, transactionRunner);
    }

    @Bean
    ProjectDeleteUseCase projectDeleteUseCase(
            ProjectJpaRepository projects,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new ProjectDeleteUseCase(projects, clock, transactionRunner);
    }

    @Bean
    ProjectPublishUseCase projectPublishUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            ProjectExportJpaRepository exports,
            TransactionRunner transactionRunner
    ) {
        return new ProjectPublishUseCase(projects, members, tracks, clips, exports, transactionRunner);
    }

    @Bean
    TrackCreateUseCase trackCreateUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new TrackCreateUseCase(projects, members, tracks, users, transactionRunner);
    }

    @Bean
    TrackGetListUseCase trackGetListUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new TrackGetListUseCase(projects, members, tracks, clips, mediaFiles, transactionRunner);
    }

    @Bean
    TrackDeleteUseCase trackDeleteUseCase(
            ProjectTrackJpaRepository tracks,
            ProjectContributionItemJpaRepository contributionItems,
            TransactionRunner transactionRunner
    ) {
        return new TrackDeleteUseCase(tracks, contributionItems, transactionRunner);
    }

    @Bean
    TrackVolumeUpdateUseCase trackVolumeUpdateUseCase(
            ProjectTrackJpaRepository tracks,
            TransactionRunner transactionRunner
    ) {
        return new TrackVolumeUpdateUseCase(tracks, transactionRunner);
    }

    @Bean
    AudioClipCreateUseCase audioClipCreateUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new AudioClipCreateUseCase(projects, members, tracks, clips, users, mediaFiles, transactionRunner);
    }

    @Bean
    MidiClipCreateUseCase midiClipCreateUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new MidiClipCreateUseCase(projects, members, tracks, clips, users, transactionRunner);
    }

    @Bean
    MidiClipSaveUseCase midiClipSaveUseCase(
            ProjectClipJpaRepository clips,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new MidiClipSaveUseCase(clips, clock, transactionRunner);
    }

    @Bean
    ProjectContributionCreateUseCase projectContributionCreateUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectContributionJpaRepository contributions,
            ProjectContributionItemJpaRepository contributionItems,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionCreateUseCase(
                projects, members, contributions, contributionItems, tracks, clips, users, transactionRunner);
    }

    @Bean
    ProjectContributionGetListUseCase projectContributionGetListUseCase(
            ProjectJpaRepository projects,
            ProjectMemberJpaRepository members,
            ProjectContributionJpaRepository contributions,
            ProfileRepositoryPort profiles,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionGetListUseCase(
                projects, members, contributions, profiles, mediaFiles, transactionRunner);
    }

    @Bean
    ProjectContributionReviewUseCase projectContributionReviewUseCase(
            ProjectContributionJpaRepository contributions,
            ProjectMemberJpaRepository members,
            ProjectTrackJpaRepository tracks,
            ProjectClipJpaRepository clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionReviewUseCase(
                contributions, members, tracks, clips, users, transactionRunner);
    }
}
