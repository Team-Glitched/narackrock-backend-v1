package glitched.adlips.application.user.admin.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Promotes the configured e-mail addresses to operator admin. Addresses that are dropped from the
 * configuration are not demoted; that is done directly in the database.
 */
public class ProvisionOperatorAdminsUseCase {

    private final UserRepositoryPort users;
    private final TransactionRunner transactions;
    private final Set<String> operatorEmails;

    public ProvisionOperatorAdminsUseCase(
            UserRepositoryPort users,
            TransactionRunner transactions,
            String operatorEmailsCsv
    ) {
        this.users = users;
        this.transactions = transactions;
        this.operatorEmails = operatorEmailsCsv == null
                ? Set.of()
                : Arrays.stream(operatorEmailsCsv.split(","))
                        .map(String::trim)
                        .map(email -> email.toLowerCase(Locale.ROOT))
                        .filter(email -> !email.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public int execute() {
        if (operatorEmails.isEmpty()) {
            return 0;
        }
        return transactions.required(() -> {
            int granted = 0;
            for (String email : operatorEmails) {
                User user = users.findByEmailIgnoreCase(email)
                        .filter(User::isActive)
                        .filter(found -> found.getRole() != UserRole.ADMIN)
                        .orElse(null);
                if (user != null) {
                    user.grantOperatorAdmin();
                    users.save(user);
                    granted++;
                }
            }
            return granted;
        });
    }
}
