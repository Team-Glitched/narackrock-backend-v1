package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanItem;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryBanType;
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

class GetGalleryBansUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryBanPort banPort;
    GetGalleryBansUseCase useCase;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        banPort = mock(GalleryBanPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(authorityPort.existsGallery(10L)).thenReturn(true);
        lenient().when(authorityPort.findActor(3L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false)));
        lenient().when(authorityPort.findActor(1L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(1L, UserRole.USER, null, false)));
        useCase = new GetGalleryBansUseCase(authorityPort, banPort, transactionRunner, CLOCK);
    }

    @Test
    void 매니저는_제재_목록을_커서로_조회한다() {
        when(banPort.findPage(10L, true, NOW, null, 2)).thenReturn(List.of(item(30L), item(20L)));

        GalleryBanPage page = useCase.execute(3L, 10L, true, null, 1);

        assertThat(page.bans()).extracting(GalleryBanItem::banId).containsExactly(30L);
        assertThat(page.nextCursor()).isEqualTo(30L);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        assertModerationError(() -> useCase.execute(1L, 10L, false, null, 20), GalleryModerationErrorCode.FORBIDDEN);
        verify(banPort, never()).findPage(anyLong(), anyBoolean(), any(), any(), anyInt());
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 404L, false, null, 20),
                GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }

    private static GalleryBanItem item(Long id) {
        return new GalleryBanItem(id, 1L, 3L, GalleryBanType.EXPEL, "광고", null, null, NOW, true);
    }
}
