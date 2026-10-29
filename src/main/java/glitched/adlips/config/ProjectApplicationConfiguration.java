package glitched.adlips.config;

import glitched.adlips.adapter.out.hti.HtiMidiRestAdapter;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.project.port.out.ProjectAudioMixerPort;
import glitched.adlips.application.project.port.out.EditorDraftRepositoryPort;
import glitched.adlips.application.project.usecase.EditorDraftUseCase;
import glitched.adlips.application.project.usecase.EditorDraftWorkflowUseCase;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionItemRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectExportRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectLayerArchivePort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.application.project.usecase.AudioClipCreateUseCase;
import glitched.adlips.application.project.usecase.HtiMidiTransformUseCase;
import glitched.adlips.application.project.usecase.MidiClipCreateUseCase;
import glitched.adlips.application.project.usecase.MidiClipSaveUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionGetListUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionNotificationUseCase;
import glitched.adlips.application.project.usecase.ProjectContributionReviewUseCase;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectDraftCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectMetadataUpdateUseCase;
import glitched.adlips.application.project.usecase.ProjectTimingUpdateUseCase;
import glitched.adlips.application.project.usecase.ProjectCompleteUseCase;
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
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
public class ProjectApplicationConfiguration {

    @Bean
    EditorDraftUseCase editorDraftUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                         ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                         UserRepositoryPort users, EditorDraftRepositoryPort drafts,
                                         MidiClipCreateUseCase createClip, MidiClipSaveUseCase saveClip,
                                         TransactionRunner transactions, Clock clock) {
        return new EditorDraftUseCase(projects, members, tracks, clips, users, drafts,
                createClip, saveClip, transactions, clock);
    }

    @Bean
    EditorDraftWorkflowUseCase editorDraftWorkflowUseCase(EditorDraftUseCase drafts, ProjectRepositoryPort projects,
                                                         ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                                         ProjectContributionCreateUseCase contributions,
                                                         TransactionRunner transactions) {
        return new EditorDraftWorkflowUseCase(drafts, projects, tracks, clips, contributions, transactions);
    }

    @Bean
    HtiMidiTransformPort htiMidiTransformPort(
            @Value("${app.hti.base-url:http://localhost:8000}") String baseUrl
    ) {
        return new HtiMidiRestAdapter(RestClient.builder().baseUrl(baseUrl).build());
    }

    @Bean
    HtiMidiTransformUseCase htiMidiTransformUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            HtiMidiTransformPort transformer
    ) {
        return new HtiMidiTransformUseCase(projects, members, transformer);
    }

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
    ProjectDraftCreateUseCase projectDraftCreateUseCase(UserRepositoryPort users, ProjectRepositoryPort projects,
                                                        ProjectMemberRepositoryPort members, TransactionRunner transactions) {
        return new ProjectDraftCreateUseCase(users, projects, members, transactions);
    }

    @Bean
    ProjectTimingUpdateUseCase projectTimingUpdateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips, EditorDraftRepositoryPort drafts,
            ProjectExportRepositoryPort exports, TransactionRunner transactions) {
        return new ProjectTimingUpdateUseCase(projects, members, tracks, clips, drafts, exports, transactions);
    }

    @Bean
    ProjectMetadataUpdateUseCase projectMetadataUpdateUseCase(ProjectRepositoryPort projects,
                                                              ProjectMemberRepositoryPort members,
                                                              MediaFileRepositoryPort mediaFiles,
                                                              TransactionRunner transactions) {
        return new ProjectMetadataUpdateUseCase(projects, members, mediaFiles, transactions);
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
    ProjectCompleteUseCase projectCompleteUseCase(
            ProjectRepositoryPort projects,
            ProjectMemberRepositoryPort members,
            PublishedShortPort publishedShorts,
            TransactionRunner transactionRunner
    ) {
        return new ProjectCompleteUseCase(projects, members, publishedShorts, transactionRunner);
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
            glitched.adlips.application.project.port.out.MidiAudioRendererPort midiRenderer,
            ProjectLayerArchivePort archiver,
            PublishedShortPort publishedShorts,
            Clock clock,
            @Value("${app.project.export-processing-batch-size:5}") int batchSize,
            TransactionRunner transactionRunner
    ) {
        return new ProcessPendingProjectExportsUseCase(
                exports, tracks, clips, mediaFiles, storage, mixer, midiRenderer, archiver,
                publishedShorts, clock, batchSize, transactionRunner);
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
    ProjectContributionNotificationUseCase projectContributionNotificationUseCase(
            ProjectContributionRepositoryPort contributions,
            ProfileRepositoryPort profiles,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionNotificationUseCase(contributions, profiles, transactionRunner);
    }

    @Bean
    ProjectContributionReviewUseCase projectContributionReviewUseCase(
            ProjectContributionRepositoryPort contributions,
            ProjectMemberRepositoryPort members,
            ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,
            UserRepositoryPort users,
            ProjectExportRepositoryPort exports,
            TransactionRunner transactionRunner
    ) {
        return new ProjectContributionReviewUseCase(
                contributions, members, tracks, clips, users, exports, transactionRunner);
    }
}
