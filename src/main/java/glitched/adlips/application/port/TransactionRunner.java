package glitched.adlips.application.port;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T required(Supplier<T> action);

    <T> T readOnly(Supplier<T> action);
}
