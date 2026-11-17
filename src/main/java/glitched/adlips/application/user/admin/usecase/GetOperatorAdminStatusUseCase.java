package glitched.adlips.application.user.admin.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;

public class GetOperatorAdminStatusUseCase {

    private final UserRepositoryPort users;
    private final TransactionRunner transactions;

    public GetOperatorAdminStatusUseCase(UserRepositoryPort users, TransactionRunner transactions) {
        this.users = users;
        this.transactions = transactions;
    }

    public boolean execute(Long userId) {
        return transactions.readOnly(() -> users.findById(userId)
                .filter(User::isActive)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false));
    }
}
