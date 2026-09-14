package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.dto.request.UserWithdrawRequest;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
public class UserWithdrawUseCase {
    private final UserRepositoryPort userRepository;
    private final RefreshTokenManager refreshTokenManager;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public UserWithdrawUseCase(
            UserRepositoryPort userRepository,
            RefreshTokenManager refreshTokenManager,
            Clock clock
    ) {
        this(userRepository, refreshTokenManager, clock, TransactionRunner.direct());
    }

    public UserWithdrawUseCase(
            UserRepositoryPort userRepository,
            RefreshTokenManager refreshTokenManager,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.refreshTokenManager = refreshTokenManager;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UserWithdrawRequest request) {
        transactionRunner.required(() -> {
            executeInternal(request);
            return null;
        });
    }

    private void executeInternal(UserWithdrawRequest request) {
        User user = userRepository.findById(request.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
        userRepository.save(user.withdraw(LocalDateTime.now(clock)));
        refreshTokenManager.revokeAll(user.getId());
    }
}
