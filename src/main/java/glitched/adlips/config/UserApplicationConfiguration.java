package glitched.adlips.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
