package glitched.adlips.config;

import glitched.adlips.adapter.out.persistence.media.MediaUploadSessionJpaRepository;
import glitched.adlips.adapter.out.storage.LocalFileStorageAdapter;
import glitched.adlips.application.media.usecase.LocalMediaContentUploadUseCase;
import glitched.adlips.application.media.usecase.MediaUploadCompleteUseCase;
import glitched.adlips.application.media.usecase.MediaUploadSessionCreateUseCase;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaApplicationConfiguration {

    @Bean
    LocalMediaContentUploadUseCase localMediaContentUploadUseCase(
            MediaFileRepositoryPort mediaFiles,
            MediaUploadSessionJpaRepository sessions,
            LocalFileStorageAdapter storage
    ) {
        return new LocalMediaContentUploadUseCase(mediaFiles, sessions, storage);
    }

    @Bean
    MediaUploadSessionCreateUseCase mediaUploadSessionCreateUseCase(
            MediaFileRepositoryPort mediaFiles,
            MediaUploadSessionJpaRepository sessions,
            Clock clock,
            @Value("${app.api.public-base-url:http://localhost:8080}") String apiBaseUrl,
            TransactionRunner transactionRunner
    ) {
        return new MediaUploadSessionCreateUseCase(
                mediaFiles, sessions, clock, apiBaseUrl, transactionRunner);
    }

    @Bean
    MediaUploadCompleteUseCase mediaUploadCompleteUseCase(
            MediaFileRepositoryPort mediaFiles,
            MediaUploadSessionJpaRepository sessions,
            LocalFileStorageAdapter storage,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        return new MediaUploadCompleteUseCase(
                mediaFiles, sessions, storage, clock, transactionRunner);
    }
}
