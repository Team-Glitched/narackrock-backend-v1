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
    /**
     * Every stored file gets a fresh key (an upload UUID or an export ID) and is not rewritten once
     * published, so clients may keep a copy for a year without revalidating. This replaces Spring
     * Security's default no-store, which made images and audio download again on every view.
     */
    static final CacheControl FILE_CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

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
        registry.addResourceHandler("/files/**").addResourceLocations(location).setCacheControl(FILE_CACHE);
    }
}
