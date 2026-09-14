package glitched.adlips.adapter.out.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.ProjectExportProcessingResult;
import glitched.adlips.application.project.usecase.ProcessPendingProjectExportsUseCase;
import org.junit.jupiter.api.Test;

class ProjectExportSchedulerTest {

    @Test
    void delegatesPendingExportProcessing() {
        ProcessPendingProjectExportsUseCase useCase = mock(ProcessPendingProjectExportsUseCase.class);
        when(useCase.execute()).thenReturn(new ProjectExportProcessingResult(1, 1, 0, 0));
        ProjectExportScheduler scheduler = new ProjectExportScheduler(useCase);

        scheduler.process();

        verify(useCase).execute();
    }
}
