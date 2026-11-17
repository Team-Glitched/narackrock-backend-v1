package glitched.adlips.config;

import glitched.adlips.adapter.out.storage.LocalFileStorageAdapter;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean(LocalFileStorageAdapter.class)
public class LocalFileWebConfiguration implements WebMvcConfigurer {
    private static final CacheControl IMMUTABLE_FILE_CACHE =
            CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final LocalFileStorageAdapter storageAdapter;

    public LocalFileWebConfiguration(LocalFileStorageAdapter storageAdapter) {
        this.storageAdapter = storageAdapter;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storageAdapter.getRootDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        // Profile images and exports use keys that are not overwritten by upload retries.
        registry.addResourceHandler("/files/profiles/**")
                .addResourceLocations(location + "profiles/")
                .setCacheControl(IMMUTABLE_FILE_CACHE);
        registry.addResourceHandler("/files/exports/**")
                .addResourceLocations(location + "exports/")
                .setCacheControl(IMMUTABLE_FILE_CACHE);
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
