package glitched.adlips.application.user.admin.usecase;

import glitched.adlips.application.user.admin.dto.request.UserBanCancelRequest;
import glitched.adlips.application.user.admin.dto.response.UserBanCancelResponse;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.admin.UserBan;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBanCancelUseCase {
    private final UserRepositoryPort userRepository;
    private final UserBanRepositoryPort userBanRepository;
    private final Clock clock;

    public UserBanCancelUseCase(
            UserRepositoryPort userRepository,
            UserBanRepositoryPort userBanRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.userBanRepository = userBanRepository;
        this.clock = clock;
    }

    @Transactional
    public UserBanCancelResponse execute(UserBanCancelRequest request) {
        if (request == null || request.adminId() == null || request.userId() == null) {
            throw new UserApplicationException(
                    UserErrorCode.VALIDATION_ERROR,
                    "차단 해제 대상 유저와 관리자는 필수입니다."
            );
        }
        requireAdmin(request.adminId());
        LocalDateTime now = LocalDateTime.now(clock);
        UserBan activeBan = userBanRepository.findActiveByUserId(request.userId(), now)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.BAN_NOT_FOUND,
                        "해제할 차단 내역이 존재하지 않습니다."
                ));
        userBanRepository.save(activeBan.lift(now));
        return new UserBanCancelResponse(request.userId());
    }

    private void requireAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new UserApplicationException(
                    UserErrorCode.FORBIDDEN_ADMIN_ACCESS,
                    "해당 기능은 관리자 전용 기능입니다. 접근 권한이 없습니다."
            );
        }
    }
}
