package glitched.adlips.application.user.admin.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.admin.dto.request.UserBanCreateRequest;
import glitched.adlips.application.user.admin.dto.response.UserBanCreateResponse;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.admin.UserBan;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.LocalDateTime;

public class UserBanCreateUseCase {

    private final UserRepositoryPort userRepository;
    private final UserBanRepositoryPort userBanRepository;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public UserBanCreateUseCase(
            UserRepositoryPort userRepository,
            UserBanRepositoryPort userBanRepository,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.userBanRepository = userBanRepository;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    public UserBanCreateUseCase(
            UserRepositoryPort userRepository,
            UserBanRepositoryPort userBanRepository,
            Clock clock
    ) {
        this(userRepository, userBanRepository, clock, TransactionRunner.direct());
    }

    public UserBanCreateResponse execute(UserBanCreateRequest request) {
        return transactionRunner.required(() -> executeInTransaction(request));
    }

    private UserBanCreateResponse executeInTransaction(UserBanCreateRequest request) {
        validate(request);

        LocalDateTime now = LocalDateTime.now(clock);
        User admin = requireAdmin(request.adminId());
        User target = requireActiveUser(
                request.userId(),
                "차단 대상 유저를 찾을 수 없습니다."
        );

        userBanRepository.findActiveByUserId(target.getId(), now)
                .ifPresent(activeBan -> {
                    throw new UserApplicationException(
                            UserErrorCode.ALREADY_BANNED_USER,
                            "이미 차단(정지) 처리가 완료된 유저입니다."
                    );
                });

        LocalDateTime bannedUntil = bannedUntil(
                request.banDurationDays(),
                now
        );

        UserBan saved = userBanRepository.save(
                UserBan.create(
                        target,
                        admin,
                        request.banReason(),
                        bannedUntil
                )
        );

        return new UserBanCreateResponse(
                target.getId(),
                request.banDurationDays(),
                saved.getBannedUntil()
        );
    }

    private void validate(UserBanCreateRequest request) {
        if (request == null
                || request.userId() == null
                || request.adminId() == null) {
            throw validation("차단 대상 유저와 관리자는 필수입니다.");
        }

        if (request.adminId().equals(request.userId())) {
            throw validation("관리자는 본인을 차단할 수 없습니다.");
        }

        if (request.banDurationDays() == null
                || request.banDurationDays() == 0
                || request.banDurationDays() < -1) {
            throw validation("차단 기간을 확인해 주세요.");
        }

        if (request.banReason() == null
                || request.banReason().isBlank()) {
            throw validation("차단 사유를 입력해 주세요.");
        }
    }

    private User requireAdmin(Long adminId) {
        User admin = requireActiveUser(
                adminId,
                "존재하지 않는 사용자입니다."
        );

        if (admin.getRole() != UserRole.ADMIN) {
            throw new UserApplicationException(
                    UserErrorCode.FORBIDDEN_ADMIN_ACCESS,
                    "해당 기능은 관리자 전용 기능입니다. 접근 권한이 없습니다."
            );
        }

        return admin;
    }

    private User requireActiveUser(Long userId, String message) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        message
                ));
    }

    private LocalDateTime bannedUntil(
            int banDurationDays,
            LocalDateTime now
    ) {
        if (banDurationDays == -1) {
            return null;
        }

        return now.plusDays(banDurationDays);
    }

    private UserApplicationException validation(String message) {
        return new UserApplicationException(
                UserErrorCode.VALIDATION_ERROR,
                message
        );
    }
}