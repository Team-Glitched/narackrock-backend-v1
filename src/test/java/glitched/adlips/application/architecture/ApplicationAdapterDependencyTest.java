package glitched.adlips.application.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.media.usecase.LocalMediaContentUploadUseCase;
import glitched.adlips.application.media.usecase.MediaUploadCompleteUseCase;
import glitched.adlips.application.media.usecase.MediaUploadSessionCreateUseCase;
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
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApplicationAdapterDependencyTest {

    @Test
    void projectAndMediaUseCasesDoNotDependOnAdapters() {
        Stream<Class<?>> useCases = Stream.of(
                LocalMediaContentUploadUseCase.class,
                MediaUploadCompleteUseCase.class,
                MediaUploadSessionCreateUseCase.class,
                AudioClipCreateUseCase.class,
                MidiClipCreateUseCase.class,
                MidiClipSaveUseCase.class,
                ProjectContributionCreateUseCase.class,
                ProjectContributionGetListUseCase.class,
                ProjectContributionReviewUseCase.class,
                ProjectCreateUseCase.class,
                ProjectDeleteUseCase.class,
                ProjectPublishUseCase.class,
                TrackCreateUseCase.class,
                TrackDeleteUseCase.class,
                TrackGetListUseCase.class,
                TrackVolumeUpdateUseCase.class
        );

        assertThat(useCases
                .flatMap(useCase -> Arrays.stream(useCase.getDeclaredConstructors()))
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .map(Class::getName))
                .noneMatch(name -> name.startsWith("glitched.adlips.adapter."));
    }
}
