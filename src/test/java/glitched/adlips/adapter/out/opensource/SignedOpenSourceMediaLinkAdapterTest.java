package glitched.adlips.adapter.out.opensource;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SignedOpenSourceMediaLinkAdapterTest {
    private static final String SECRET="opensource-test-secret-at-least-32-characters";
    private static final Instant NOW=Instant.parse("2026-10-29T00:00:00Z");
    private SignedOpenSourceMediaLinkAdapter adapter(Instant now) {
        return new SignedOpenSourceMediaLinkAdapter("https://api.example/", "https://app.example/opensource/", SECRET,600,Clock.fixed(now,ZoneOffset.UTC));
    }
    @Test void signatureIsBoundToSourceMediaAndExpiryAndExpiresAtBoundary() {
        var issuer=adapter(NOW);
        var link=issuer.playback(1L,2L);
        String signature=link.url().split("signature=")[1];
        long expires=link.expiresAt().getEpochSecond();
        assertThat(expires).isEqualTo(NOW.getEpochSecond()+600);
        assertThat(issuer.verify(1L,2L,expires,signature)).isTrue();
        assertThat(adapter(NOW.plusSeconds(599)).verify(1L,2L,expires,signature)).isTrue();
        assertThat(adapter(NOW.plusSeconds(600)).verify(1L,2L,expires,signature)).isFalse();
        assertThat(issuer.verify(2L,2L,expires,signature)).isFalse();
        assertThat(issuer.verify(1L,3L,expires,signature)).isFalse();
        assertThat(issuer.verify(1L,2L,expires+1,signature)).isFalse();
        assertThat(issuer.verify(1L,2L,expires,null)).isFalse();
        assertThat(issuer.share(1L)).isEqualTo("https://app.example/opensource/1");
    }
    @Test void invalidConfigurationFailsEarly() {
        for(String base: java.util.List.of("javascript:bad", "https://user:password@example.com", "https://example.com?a=1", "https://example.com/#x"))
            assertThatThrownBy(() -> new SignedOpenSourceMediaLinkAdapter(base,"https://app.example",SECRET,600,Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignedOpenSourceMediaLinkAdapter("https://api.example","https://app.example",SECRET,0,Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
    }
}
