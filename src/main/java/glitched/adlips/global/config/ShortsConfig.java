package glitched.adlips.global.config;

import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.ShortCommentRepository;
import glitched.adlips.application.port.ShortFormRepository;
import glitched.adlips.application.port.ShortInteractionRepository;
import glitched.adlips.application.port.ShortParticipantRepository;
import glitched.adlips.application.shorts.GetShortsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShortsConfig {

    @Bean
    public GetShortsUseCase getShortsUseCase(
            ShortFormRepository shortFormRepository,
            ShortParticipantRepository shortParticipantRepository,
            ShortInteractionRepository shortInteractionRepository,
            ShortCommentRepository shortCommentRepository,
            ProfileRepository profileRepository
    ) {
        return new GetShortsUseCase(
                shortFormRepository,
                shortParticipantRepository,
                shortInteractionRepository,
                shortCommentRepository,
                profileRepository
        );
    }
}
