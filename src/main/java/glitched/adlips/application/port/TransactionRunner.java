package glitched.adlips.application.port;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T required(Supplier<T> action);

    <T> T readOnly(Supplier<T> action);

    default <T> T requiredWithoutRollbackOn(
            Class<? extends RuntimeException> noRollbackException,
            Supplier<T> action
    ) {
        return required(action);
    }

    static TransactionRunner direct() {
        return new TransactionRunner() {
            @Override
            public <T> T required(Supplier<T> action) {
                return action.get();
            }

            @Override
            public <T> T readOnly(Supplier<T> action) {
                return action.get();
            }
        };
    }
}
