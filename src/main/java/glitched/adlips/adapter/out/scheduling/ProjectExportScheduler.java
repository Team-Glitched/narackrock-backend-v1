package glitched.adlips.adapter.out.scheduling;

import glitched.adlips.application.project.ProjectExportProcessingResult;
import glitched.adlips.application.project.usecase.ProcessPendingProjectExportsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.project.export-processing-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProjectExportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProjectExportScheduler.class);

    private final ProcessPendingProjectExportsUseCase useCase;

    public ProjectExportScheduler(ProcessPendingProjectExportsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(
            fixedDelayString = "${app.project.export-processing-interval:PT5S}",
            initialDelayString = "${app.project.export-processing-initial-delay:PT5S}"
    )
    public void process() {
        ProjectExportProcessingResult result = useCase.execute();
        if (result.failedCount() > 0) {
            log.warn("프로젝트 Export 일부 실패: completed={}, failed={}, skipped={}",
                    result.completedCount(), result.failedCount(), result.skippedCount());
        } else if (result.completedCount() > 0) {
            log.info("프로젝트 Export 완료: completed={}, skipped={}",
                    result.completedCount(), result.skippedCount());
        }
    }
}
