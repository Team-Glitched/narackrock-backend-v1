package glitched.adlips.config;

import glitched.adlips.application.opensource.OpenSourceSavePort;
import glitched.adlips.application.opensource.OpenSourceSaveUseCase;
import glitched.adlips.application.port.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSourceApplicationConfiguration {
    @Bean
    OpenSourceSaveUseCase openSourceSaveUseCase(OpenSourceSavePort port, TransactionRunner transactions) {
        return new OpenSourceSaveUseCase(port, transactions);
    }
}
