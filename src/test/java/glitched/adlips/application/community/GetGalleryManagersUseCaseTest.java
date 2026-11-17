package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryManagerItem;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
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

class GetGalleryManagersUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GalleryManagementPort managementPort;
    GetGalleryManagersUseCase useCase;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        managementPort = mock(GalleryManagementPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(authorityPort.existsGallery(10L)).thenReturn(true);
        useCase = new GetGalleryManagersUseCase(authorityPort, managementPort, transactionRunner, CLOCK);
    }

    @Test
    void 커뮤니티_어드민은_목록을_받는다() {
        when(authorityPort.findActor(3L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false)));
        List<GalleryManagerItem> managers = List.of(new GalleryManagerItem(3L, "재즈맨", null, NOW.minusDays(1)));
        when(managementPort.findManagers(10L)).thenReturn(managers);

        assertThat(useCase.execute(3L, 10L)).isEqualTo(managers);
    }

    @Test
    void 권한이_없으면_FORBIDDEN이다() {
        when(authorityPort.findActor(2L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(2L, UserRole.USER, null, false)));

        assertModerationError(() -> useCase.execute(2L, 10L), GalleryModerationErrorCode.FORBIDDEN);
        verify(managementPort, never()).findManagers(anyLong());
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        assertModerationError(() -> useCase.execute(3L, 404L), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }
}
