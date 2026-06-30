package glitched.adlips.application.user.port.out;

import java.util.function.Supplier;

public interface TransactionPort {
    <T> T required(Supplier<T> operation);

    default void required(Runnable operation) {
        required(() -> {
            operation.run();
            return null;
        });
    }
}
