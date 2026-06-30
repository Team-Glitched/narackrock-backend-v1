package glitched.adlips.adapter.out.profile;

import glitched.adlips.application.user.port.out.ProfileLinkPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredProfileLinkAdapter implements ProfileLinkPort {
    private final String baseUrl;

    public ConfiguredProfileLinkAdapter(@Value("${app.profile.share-base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String create(Long userId) {
        return baseUrl + "/" + userId;
    }
}
