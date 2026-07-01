package glitched.adlips.config;

import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShortsApplicationConfiguration {
    @Bean
    GetShortsUseCase getShortsUseCase(ShortsQueryPort shortsQueryPort) {
        return new GetShortsUseCase(shortsQueryPort);
    }
}
