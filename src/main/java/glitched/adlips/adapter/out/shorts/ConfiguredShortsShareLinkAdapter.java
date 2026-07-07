package glitched.adlips.adapter.out.shorts;

import glitched.adlips.application.shorts.port.out.ShortsShareLinkPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredShortsShareLinkAdapter implements ShortsShareLinkPort {

    private final String baseUrl;

    public ConfiguredShortsShareLinkAdapter(@Value("${app.shorts.share-base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String create(Long shortId) {
        return baseUrl + "/" + shortId;
    }
}
