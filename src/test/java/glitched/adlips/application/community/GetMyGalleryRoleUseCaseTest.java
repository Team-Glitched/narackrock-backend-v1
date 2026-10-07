package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetMyGalleryRoleUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    GalleryAuthorityPort authorityPort;
    GetMyGalleryRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        lenient().when(transactionRunner.readOnly(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        lenient().when(authorityPort.existsGallery(10L)).thenReturn(true);
        useCase = new GetMyGalleryRoleUseCase(authorityPort, transactionRunner, CLOCK);
    }

    @Test
    void 매니저는_관리_가능으로_표시된다() {
        when(authorityPort.findActor(3L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false)));

        GalleryRoleResult result = useCase.execute(3L, 10L);

        assertThat(result).isEqualTo(new GalleryRoleResult(10L, GalleryRole.MANAGER, false, true));
    }

    @Test
    void 일반_유저는_역할이_없고_관리_불가로_표시된다() {
        when(authorityPort.findActor(1L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(1L, UserRole.USER, null, false)));

        assertThat(useCase.execute(1L, 10L)).isEqualTo(new GalleryRoleResult(10L, null, false, false));
    }

    @Test
    void 어드민은_역할이_없어도_관리_가능으로_표시된다() {
        when(authorityPort.findActor(9L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(9L, UserRole.ADMIN, null, false)));

        assertThat(useCase.execute(9L, 10L)).isEqualTo(new GalleryRoleResult(10L, null, true, true));
    }

    @Test
    void 사용자를_찾을_수_없으면_FORBIDDEN이다() {
        when(authorityPort.findActor(5L, 10L, NOW)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(5L, 10L), GalleryModerationErrorCode.FORBIDDEN);
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        when(authorityPort.existsGallery(404L)).thenReturn(false);

        assertModerationError(() -> useCase.execute(1L, 404L), GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }
}
