package glitched.adlips.config;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.project.port.out.ProjectAudioMixerPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionItemRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.usecase.AudioClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipSaveUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionGetListUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectDeleteUseCase;
import glitched.adlips.application.project.usecase.ProjectExportGetUseCase;
import glitched.adlips.application.project.usecase.ProcessPendingProjectExportsUseCase;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ProjectApplicationConfiguration {

    @Bean
    ProjectCreateUseCase projectCreateUseCase(
            UserRepositoryPort users,
            MediaFileRepositoryPort mediaFiles,
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            TransactionRunner transactionRunner
    ) {
        return new ProjectCreateUseCase(users, mediaFiles, projects, members, transactionRunner);
    }

    @Bean
    ProjectDeleteUseCase projectDeleteUseCase(
            ProjectRepositoryPort projects,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new ProjectDeleteUseCase(projects, clock, transactionRunner);
    }

    @Bean
    ProjectPublishUseCase projectPublishUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            ProjectExportRepositoryPort exports,
            TransactionRunner transactionRunner
    ) {
        return new ProjectPublishUseCase(projects, members, tracks, clips, exports, transactionRunner);
    }

    @Bean
    ProjectExportGetUseCase projectExportGetUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectExportRepositoryPort exports,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new ProjectExportGetUseCase(projects, members, exports, mediaFiles, transactionRunner);
    }

    @Bean
    ProcessPendingProjectExportsUseCase processPendingProjectExportsUseCase(
            ProjectExportRepositoryPort exports,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            MediaFileRepositoryPort mediaFiles,
            MediaContentStoragePort storage,
            ProjectAudioMixerPort mixer,
            ProjectLayerArchivePort archiver,
            Clock clock,
            @Value("${app.project.export-processing-batch-size:5}") int batchSize,
            TransactionRunner transactionRunner
    ) {
        return new ProcessPendingProjectExportsUseCase(
                exports, tracks, clips, mediaFiles, storage, mixer, archiver,
                clock, batchSize, transactionRunner);
    }

    @Bean
    TrackCreateUseCase trackCreateUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new TrackCreateUseCase(projects, members, tracks, users, transactionRunner);
    }

    @Bean
    TrackGetListUseCase trackGetListUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new TrackGetListUseCase(projects, members, tracks, clips, mediaFiles, transactionRunner);
    }

    @Bean
    TrackDeleteUseCase trackDeleteUseCase(
            ProjectTrackRepositoryPort tracks,
            ProjectContributionItemRepositoryPort contributionItems,
            TransactionRunner transactionRunner
    ) {
        return new TrackDeleteUseCase(tracks, contributionItems, transactionRunner);
    }

    @Bean
    TrackVolumeUpdateUseCase trackVolumeUpdateUseCase(
            ProjectTrackRepositoryPort tracks,
            TransactionRunner transactionRunner
    ) {
        return new TrackVolumeUpdateUseCase(tracks, transactionRunner);
    }

    @Bean
    AudioClipCreateUseCase audioClipCreateUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new AudioClipCreateUseCase(projects, members, tracks, clips, users, mediaFiles, transactionRunner);
    }

    @Bean
    MidiClipCreateUseCase midiClipCreateUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new MidiClipCreateUseCase(projects, members, tracks, clips, users, transactionRunner);
    }

    @Bean
    MidiClipSaveUseCase midiClipSaveUseCase(
            ProjectClipRepositoryPort clips,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new MidiClipSaveUseCase(clips, clock, transactionRunner);
    }

    @Bean
    ProjectContributionCreateUseCase projectContributionCreateUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectContributionRepositoryPort contributions,
            ProjectContributionItemRepositoryPort contributionItems,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionCreateUseCase(
                projects, members, contributions, contributionItems, tracks, clips, users, transactionRunner);
    }

    @Bean
    ProjectContributionGetListUseCase projectContributionGetListUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            ProjectContributionRepositoryPort contributions,
            ProfileRepositoryPort profiles,
            MediaFileRepositoryPort mediaFiles,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionGetListUseCase(
                projects, members, contributions, profiles, mediaFiles, transactionRunner);
    }

    @Bean
    ProjectContributionReviewUseCase projectContributionReviewUseCase(
            ProjectContributionRepositoryPort contributions,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionReviewUseCase(
                contributions, members, tracks, clips, users, transactionRunner);
    }
}
