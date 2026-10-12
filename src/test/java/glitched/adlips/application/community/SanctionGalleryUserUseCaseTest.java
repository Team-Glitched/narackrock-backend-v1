package glitched.adlips.application.community;

import static glitched.adlips.application.community.UpdateGalleryUseCaseTest.assertModerationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryBan;
import glitched.adlips.domain.community.GalleryBanType;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryRole;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SanctionGalleryUserUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private static final GalleryActor MANAGER = new GalleryActor(3L, UserRole.USER, GalleryRole.MANAGER, false);
    private static final GalleryActor TARGET = new GalleryActor(1L, UserRole.USER, null, false);

    GalleryAuthorityPort authorityPort;
    GalleryManagementPort managementPort;
    GalleryBanPort banPort;
    GallerySubscriptionPort subscriptionPort;
    UserRepositoryPort userRepository;
    GalleryModerationLogPort logPort;
    SanctionGalleryUserUseCase useCase;
    List<String> calls;

    @BeforeEach
    void setUp() {
        authorityPort = mock(GalleryAuthorityPort.class);
        managementPort = mock(GalleryManagementPort.class);
        banPort = mock(GalleryBanPort.class);
        subscriptionPort = mock(GallerySubscriptionPort.class);
        userRepository = mock(UserRepositoryPort.class);
        logPort = mock(GalleryModerationLogPort.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        calls = new ArrayList<>();
        lenient().when(transactionRunner.required(any())).thenAnswer(invocation -> {
            calls.add("tx-begin");
            Object result = invocation.<Supplier<?>>getArgument(0).get();
            calls.add("tx-end");
            return result;
        });
        lenient().when(managementPort.findGalleryForUpdate(10L))
                .thenReturn(Optional.of(Gallery.create("재즈", "설명")));
        lenient().when(authorityPort.findActor(3L, 10L, NOW)).thenReturn(Optional.of(MANAGER));
        lenient().when(authorityPort.findActor(1L, 10L, NOW)).thenReturn(Optional.of(TARGET));
        lenient().when(userRepository.findById(3L))
                .thenReturn(Optional.of(User.restore(3L, "m@test.com", UserRole.USER, null)));
        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.restore(1L, "u@test.com", UserRole.USER, null)));
        lenient().when(banPort.save(any())).thenAnswer(invocation -> {
            calls.add("save-ban");
            return 70L;
        });
        lenient().doAnswer(invocation -> calls.add("log")).when(logPort)
                .save(any(), any(), any(), any(), any(), any());
        useCase = new SanctionGalleryUserUseCase(authorityPort, managementPort, banPort, subscriptionPort,
                userRepository, logPort, transactionRunner, CLOCK);
    }

    @Test
    void 이용정지는_기간을_정해_저장하고_이력을_남긴다() {
        Long banId = useCase.execute(3L, 10L, 1L, "SUSPEND", 7, "도배");

        assertThat(banId).isEqualTo(70L);
        GalleryBan saved = captureSavedBan();
        assertThat(saved.getBanType()).isEqualTo(GalleryBanType.SUSPEND);
        assertThat(saved.getBannedUntil()).isEqualTo(NOW.plusDays(7));
        verify(logPort).save(10L, 3L, GalleryModerationActionType.SUSPEND_USER, 1L, 70L, "도배");
        verify(subscriptionPort, never()).deleteSubscription(anyLong(), anyLong());
        assertThat(calls).containsExactly("tx-begin", "save-ban", "log", "tx-end");
    }

    @Test
    void 추방은_구독을_해제한다() {
        doAnswer(invocation -> calls.add("unsubscribe")).when(subscriptionPort).deleteSubscription(1L, 10L);

        useCase.execute(3L, 10L, 1L, "EXPEL", null, "광고");

        assertThat(captureSavedBan().getBanType()).isEqualTo(GalleryBanType.EXPEL);
        verify(logPort).save(10L, 3L, GalleryModerationActionType.EXPEL_USER, 1L, 70L, "광고");
        assertThat(calls).containsExactly("tx-begin", "save-ban", "unsubscribe", "log", "tx-end");
    }

    @Test
    void 일반_유저는_FORBIDDEN이다() {
        when(authorityPort.findActor(2L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(2L, UserRole.USER, null, false)));

        assertModerationError(() -> useCase.execute(2L, 10L, 1L, "SUSPEND", 7, "사유"),
                GalleryModerationErrorCode.FORBIDDEN);
        verify(banPort, never()).save(any());
    }

    @Test
    void 다른_갤러리의_매니저는_FORBIDDEN이다() {
        when(authorityPort.findActor(4L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(4L, UserRole.USER, null, false)));

        assertModerationError(() -> useCase.execute(4L, 10L, 1L, "EXPEL", null, "사유"),
                GalleryModerationErrorCode.FORBIDDEN);
    }

    @Test
    void 매니저나_어드민이나_본인은_제재할_수_없다() {
        when(authorityPort.findActor(9L, 10L, NOW))
                .thenReturn(Optional.of(new GalleryActor(9L, UserRole.ADMIN, null, false)));

        assertModerationError(() -> useCase.execute(3L, 10L, 9L, "EXPEL", null, "사유"),
                GalleryModerationErrorCode.CANNOT_SANCTION_TARGET);
        assertModerationError(() -> useCase.execute(3L, 10L, 3L, "EXPEL", null, "사유"),
                GalleryModerationErrorCode.CANNOT_SANCTION_TARGET);
        verify(banPort, never()).save(any());
    }

    @Test
    void 대상_사용자가_없으면_TARGET_USER_NOT_FOUND다() {
        when(authorityPort.findActor(404L, 10L, NOW)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(3L, 10L, 404L, "EXPEL", null, "사유"),
                GalleryModerationErrorCode.TARGET_USER_NOT_FOUND);
    }

    @Test
    void 이미_활성_제재가_있으면_ALREADY_BANNED_IN_GALLERY다() {
        when(banPort.existsActive(10L, 1L, NOW)).thenReturn(true);

        assertModerationError(() -> useCase.execute(3L, 10L, 1L, "SUSPEND", 1, "사유"),
                GalleryModerationErrorCode.ALREADY_BANNED_IN_GALLERY);
        verify(banPort, never()).save(any());
    }

    @Test
    void 입력이_잘못되면_VALIDATION_ERROR다() {
        assertModerationError(() -> useCase.execute(3L, 10L, 1L, "WRONG", 1, "사유"),
                GalleryModerationErrorCode.VALIDATION_ERROR);
        assertModerationError(() -> useCase.execute(3L, 10L, 1L, null, 1, "사유"),
                GalleryModerationErrorCode.VALIDATION_ERROR);
        assertModerationError(() -> useCase.execute(3L, 10L, 1L, "SUSPEND", null, "사유"),
                GalleryModerationErrorCode.VALIDATION_ERROR);
        assertModerationError(() -> useCase.execute(3L, 10L, 1L, "SUSPEND", 31, "사유"),
                GalleryModerationErrorCode.VALIDATION_ERROR);
        assertModerationError(() -> useCase.execute(3L, 10L, 1L, "EXPEL", null, " "),
                GalleryModerationErrorCode.VALIDATION_ERROR);
        verify(banPort, never()).save(any());
    }

    @Test
    void 갤러리가_없으면_GALLERY_NOT_FOUND다() {
        when(managementPort.findGalleryForUpdate(404L)).thenReturn(Optional.empty());

        assertModerationError(() -> useCase.execute(3L, 404L, 1L, "EXPEL", null, "사유"),
                GalleryModerationErrorCode.GALLERY_NOT_FOUND);
    }

    private GalleryBan captureSavedBan() {
        ArgumentCaptor<GalleryBan> captor = ArgumentCaptor.forClass(GalleryBan.class);
        verify(banPort).save(captor.capture());
        return captor.getValue();
    }
}
