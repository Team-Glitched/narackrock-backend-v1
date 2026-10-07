package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetGalleryModerationLogsUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryModerationLogPort logPort;
    GetGalleryModerationLogsUseCase useCase;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(authorityPort.existsGallery(10L)).thenReturn(true);
        lenient().when(authorityPort.findActor(3L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false)));
        lenient().when(authorityPort.findActor(1L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(1L, UserRole.USER, null, false)));
        useCase = new GetGalleryModerationLogsUseCase(authorityPort, logPort, transactionRunner, CLOCK);
    }

    @Test
    void 매니저는_이력을_커서로_조회한다() {
        when(logPort.findPage(10L, null, 3)).thenReturn(List.of(item(30L), item(20L), item(10L)));

        GalleryModerationLogPage page = useCase.execute(3L, 10L, null, 2);

        assertThat(page.logs()).extracting(GalleryModerationLogItem::logId).containsExactly(30L, 20L);
        assertThat(page.nextCursor()).isEqualTo(20L);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        assertModerationError(() -> useCase.execute(1L, 10L, null, 20), GalleryModerationErrorCode.FORBIDDEN);
        verify(logPort, never()).findPage(anyLong(), any(), anyInt());
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 404L, null, 20), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }

    @Test
    void 페이지_값이_잘못되면_VALIDATION_ERROR다() {
        assertModerationError(() -> useCase.execute(3L, 10L, 0L, 20), GalleryModerationErrorCode.VALIDATION_ERROR);
        assertModerationError(() -> useCase.execute(3L, 10L, null, 101), GalleryModerationErrorCode.VALIDATION_ERROR);
    }

    private static GalleryModerationLogItem item(Long id) {
        return new GalleryModerationLogItem(id, GalleryModerationActionType.DELETE_POST, 3L, 1L, 99L, "광고",
                LocalDateTime.of(2026, 10, 7, 12, 0));
    }
}
