package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.dto.request.UserWithdrawRequest;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWithdrawUseCase {
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    public UserWithdrawUseCase(UserRepositoryPort userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void execute(UserWithdrawRequest request) {
        User user = userRepository.findById(request.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
        userRepository.save(user.withdraw(LocalDateTime.now(clock)));
    }
}
