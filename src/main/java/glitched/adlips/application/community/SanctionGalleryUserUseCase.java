package glitched.adlips.application.community;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

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
import glitched.adlips.domain.community.GalleryModerationPolicy;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;

public class SanctionGalleryUserUseCase {
    private final NotificationEvents notifications;

    private final GalleryAuthorityPort authorityPort;
    private final GalleryManagementPort managementPort;
    private final GalleryBanPort banPort;
    private final GallerySubscriptionPort subscriptionPort;
    private final UserRepositoryPort userRepository;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public SanctionGalleryUserUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryManagementPort managementPort,
            GalleryBanPort banPort,
            GallerySubscriptionPort subscriptionPort,
            UserRepositoryPort userRepository,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(authorityPort, managementPort, banPort, subscriptionPort, userRepository, logPort, transactionRunner, clock, NotificationEvents.none());
    }

    public SanctionGalleryUserUseCase(GalleryAuthorityPort authorityPort,
            GalleryManagementPort managementPort,
            GalleryBanPort banPort,
            GallerySubscriptionPort subscriptionPort,
            UserRepositoryPort userRepository,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock, NotificationEvents notifications) {
        this.authorityPort = authorityPort;
        this.managementPort = managementPort;
        this.banPort = banPort;
        this.subscriptionPort = subscriptionPort;
        this.userRepository = userRepository;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    
        this.notifications = notifications;
    }

    /** 갤러리 행을 잠가 같은 갤러리의 제재를 직렬화한다(활성 제재 1건 보장). */
    public Long execute(
            Long actorId, Long galleryId, Long targetUserId, String typeValue, Integer durationDays, String reason
    ) {
        return transactionRunner.required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            Gallery gallery = managementPort.findGalleryForUpdate(galleryId)
                    .orElseThrow(GalleryModerationGuard::galleryNotFound);
            GalleryActor actor = GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            GalleryBanType type = validate(typeValue, durationDays, reason);

            GalleryActor target = authorityPort.findActor(targetUserId, galleryId, now)
                    .orElseThrow(() -> new GalleryModerationApplicationException(
                            GalleryModerationErrorCode.TARGET_USER_NOT_FOUND, "제재할 사용자를 찾을 수 없습니다."));
            if (!GalleryModerationPolicy.canSanction(actor, target)) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.CANNOT_SANCTION_TARGET,
                        "본인, 갤러리 매니저, 관리자는 제재할 수 없습니다.");
            }
            if (banPort.existsActive(galleryId, targetUserId, now)) {
                throw new GalleryModerationApplicationException(
                        GalleryModerationErrorCode.ALREADY_BANNED_IN_GALLERY, "이미 이 갤러리에서 제재 중인 사용자입니다.");
            }

            User targetUser = userRepository.findById(targetUserId).orElseThrow();
            User actorUser = userRepository.findById(actorId).orElseThrow();
            GalleryBan ban = type == GalleryBanType.SUSPEND
                    ? GalleryBan.suspend(gallery, targetUser, actorUser, reason, durationDays, now)
                    : GalleryBan.expel(gallery, targetUser, actorUser, reason);
            Long banId = banPort.save(ban);
            if (type == GalleryBanType.EXPEL) {
                subscriptionPort.deleteSubscription(targetUserId, galleryId);
            }
            logPort.save(galleryId, actorId, actionType(type), targetUserId, banId, ban.getReason());
            notifications.record(type==GalleryBanType.SUSPEND?GALLERY_SUSPENDED:GALLERY_EXPELLED,actorId,banId);
            return banId;
        });
    }

    private static GalleryBanType validate(String typeValue, Integer durationDays, String reason) {
        GalleryBanType type;
        try {
            type = GalleryBanType.valueOf(typeValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw validationError("제재 유형은 SUSPEND 또는 EXPEL이어야 합니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw validationError("제재 사유를 입력해 주세요.");
        }
        if (type == GalleryBanType.SUSPEND && (durationDays == null
                || durationDays < GalleryBan.MIN_SUSPEND_DAYS || durationDays > GalleryBan.MAX_SUSPEND_DAYS)) {
            throw validationError("이용정지 기간은 1일 이상 30일 이하여야 합니다.");
        }
        return type;
    }

    private static GalleryModerationActionType actionType(GalleryBanType type) {
        return type == GalleryBanType.SUSPEND
                ? GalleryModerationActionType.SUSPEND_USER
                : GalleryModerationActionType.EXPEL_USER;
    }

    private static GalleryModerationApplicationException validationError(String message) {
        return new GalleryModerationApplicationException(GalleryModerationErrorCode.VALIDATION_ERROR, message);
    }
}
