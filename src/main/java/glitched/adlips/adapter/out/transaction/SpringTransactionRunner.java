package glitched.adlips.adapter.out.transaction;

import glitched.adlips.application.port.TransactionRunner;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate requiredTransaction;
    private final TransactionTemplate readOnlyTransaction;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.requiredTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    @Override
    public <T> T required(Supplier<T> action) {
        return requiredTransaction.execute(status -> action.get());
    }

    @Override
    public <T> T readOnly(Supplier<T> action) {
        return readOnlyTransaction.execute(status -> action.get());
    }
}
