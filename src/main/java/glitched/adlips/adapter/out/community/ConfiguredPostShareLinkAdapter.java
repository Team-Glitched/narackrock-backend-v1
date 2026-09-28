package glitched.adlips.adapter.out.community;

import glitched.adlips.application.community.port.out.PostShareLinkPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredPostShareLinkAdapter implements PostShareLinkPort {

    private final String baseUrl;

    public ConfiguredPostShareLinkAdapter(@Value("${app.posts.share-base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String create(Long postId) {
        return baseUrl + "/" + postId;
    }
}
