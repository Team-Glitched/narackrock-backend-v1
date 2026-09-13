package glitched.adlips.application.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.view.port.out.ContentViewPendingPort;
import glitched.adlips.application.view.port.out.ContentViewPersistencePort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncPendingContentViewsUseCaseTest {

    @Mock ContentViewPendingPort pendingPort;
    @Mock ContentViewPersistencePort persistencePort;

    SyncPendingContentViewsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncPendingContentViewsUseCase(
                pendingPort, persistencePort, TransactionRunner.direct(), 100);
    }

    @Test
    void Redis_증가분을_DB에_반영한_뒤_완료_처리한다() {
        ContentViewKey key = new ContentViewKey(ContentViewTarget.SHORT, 12L);
        when(pendingPort.findPending(100)).thenReturn(List.of(key));
        when(pendingPort.claim(key)).thenReturn(new ClaimedContentViews("batch-1", 4L));

        ContentViewSyncResult result = useCase.execute();

        InOrder order = inOrder(persistencePort, pendingPort);
        order.verify(persistencePort).increment(key, 4L);
        order.verify(pendingPort).complete(key, "batch-1", 4L);
        assertThat(result).isEqualTo(new ContentViewSyncResult(1, 4L, 0));
    }

    @Test
    void DB_반영이_실패하면_증가분을_Redis로_복구한다() {
        ContentViewKey key = new ContentViewKey(ContentViewTarget.POST, 501L);
        when(pendingPort.findPending(100)).thenReturn(List.of(key));
        when(pendingPort.claim(key)).thenReturn(new ClaimedContentViews("batch-2", 3L));
        doThrow(new IllegalStateException("database unavailable"))
                .when(persistencePort).increment(key, 3L);

        ContentViewSyncResult result = useCase.execute();

        verify(pendingPort).restore(key, "batch-2", 3L);
        assertThat(result).isEqualTo(new ContentViewSyncResult(0, 0L, 1));
    }

    @Test
    void 다른_작업자가_선점한_대상은_건너뛴다() {
        ContentViewKey key = new ContentViewKey(ContentViewTarget.SHORT, 12L);
        when(pendingPort.findPending(100)).thenReturn(List.of(key));
        when(pendingPort.claim(key)).thenReturn(ClaimedContentViews.empty());

        ContentViewSyncResult result = useCase.execute();

        assertThat(result).isEqualTo(new ContentViewSyncResult(0, 0L, 0));
    }
}
