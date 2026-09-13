package glitched.adlips.adapter.out.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.view.ContentViewSyncResult;
import glitched.adlips.application.view.SyncPendingContentViewsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentViewSyncSchedulerTest {

    @Mock SyncPendingContentViewsUseCase useCase;

    @Test
    void 설정된_주기마다_대기중인_조회수를_동기화한다() {
        when(useCase.execute()).thenReturn(new ContentViewSyncResult(2, 7L, 0));
        ContentViewSyncScheduler scheduler = new ContentViewSyncScheduler(useCase);

        scheduler.synchronize();

        verify(useCase).execute();
    }
}
