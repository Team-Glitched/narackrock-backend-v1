package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.*;
import glitched.adlips.adapter.out.transaction.SpringTransactionRunner;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({OpenSourceSavePersistenceAdapter.class, SpringTransactionRunner.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OpenSourceSavePersistenceTest {
    @Autowired OpenSourceSavePersistenceAdapter adapter;
    @Autowired SpringTransactionRunner transactions;
    @Autowired JdbcTemplate jdbc;
    OpenSourceSaveUseCase useCase;

    @BeforeEach void setup() {
        useCase = new OpenSourceSaveUseCase(adapter, transactions);
        jdbc.update("INSERT INTO open_sources (id, is_public, owner_id, title, media_file_id, created_at, updated_at) VALUES (88001, true, 1, 'test', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM open_source_saves WHERE open_source_id = 88001");
        jdbc.update("DELETE FROM open_sources WHERE id = 88001");
    }
    @Test void repeatedSavePreservesTimestampAndRemovalIsUserScoped() {
        useCase.save(1L, 88001L);
        Object savedAt = jdbc.queryForObject("SELECT saved_at FROM open_source_saves WHERE user_id = 1", Object.class);
        useCase.save(1L, 88001L);
        useCase.save(2L, 88001L);
        assertThat(jdbc.queryForObject("SELECT saved_at FROM open_source_saves WHERE user_id = 1", Object.class)).isEqualTo(savedAt);
        useCase.remove(1L, 88001L);
        useCase.remove(1L, 88001L);
        assertThat(adapter.isSaved(1L, 88001L)).isFalse();
        assertThat(adapter.isSaved(2L, 88001L)).isTrue();
    }
    @Test void hiddenAndDeletedSourcesRejectSaveButAllowRemoval() {
        useCase.save(1L, 88001L);
        jdbc.update("UPDATE open_sources SET is_public = false WHERE id = 88001");
        assertThatThrownBy(() -> useCase.save(2L, 88001L)).isInstanceOf(OpenSourceException.class);
        jdbc.update("UPDATE open_sources SET is_public = true, deleted_at = CURRENT_TIMESTAMP WHERE id = 88001");
        assertThatThrownBy(() -> useCase.save(2L, 88001L)).isInstanceOf(OpenSourceException.class);
        useCase.remove(1L, 88001L);
        assertThat(adapter.isSaved(1L, 88001L)).isFalse();
        assertThat(useCase.remove(1L, 99999L).isSaved()).isFalse();
    }
    @Test void removedSourceStillAllowsClearingSavedEntry() {
        useCase.save(1L, 88001L);
        jdbc.update("DELETE FROM open_sources WHERE id = 88001");
        useCase.remove(1L, 88001L);
        assertThat(adapter.isSaved(1L, 88001L)).isFalse();
    }
    @Test void databaseRejectsDuplicateRelations() {
        useCase.save(1L, 88001L);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO open_source_saves (user_id, open_source_id, saved_at) VALUES (1, 88001, CURRENT_TIMESTAMP)"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void concurrentSavesBothSucceedAndCreateOneRow() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Callable<OpenSourceSaveResult> action = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                return useCase.save(1L, 88001L);
            };
            var first = executor.submit(action);
            var second = executor.submit(action);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).isSaved()).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS).isSaved()).isTrue();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM open_source_saves WHERE open_source_id = 88001", Integer.class)).isEqualTo(1);
        }
    }
}
