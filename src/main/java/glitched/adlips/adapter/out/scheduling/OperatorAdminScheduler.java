package glitched.adlips.adapter.out.scheduling;

import glitched.adlips.application.user.admin.usecase.ProvisionOperatorAdminsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OperatorAdminScheduler {

    private static final Logger log = LoggerFactory.getLogger(OperatorAdminScheduler.class);

    private final ProvisionOperatorAdminsUseCase useCase;

    public OperatorAdminScheduler(ProvisionOperatorAdminsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(
            fixedDelayString = "${app.admin.operator-sync-interval:PT1M}",
            initialDelayString = "${app.admin.operator-sync-initial-delay:PT5S}"
    )
    public void provision() {
        int count = useCase.execute();
        if (count > 0) {
            log.info("운영자 어드민 지정: {}명", count);
        }
    }
}
