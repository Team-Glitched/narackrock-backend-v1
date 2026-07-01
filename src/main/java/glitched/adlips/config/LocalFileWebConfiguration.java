package glitched.adlips.config;

import glitched.adlips.adapter.out.storage.LocalFileStorageAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean(LocalFileStorageAdapter.class)
public class LocalFileWebConfiguration implements WebMvcConfigurer {
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
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
