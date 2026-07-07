package glitched.adlips.adapter.out.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfiguredShortsShareLinkAdapterTest {

    @Test
    void base_url_끝에_슬래시가_없으면_그대로_붙인다() {
        ConfiguredShortsShareLinkAdapter adapter =
                new ConfiguredShortsShareLinkAdapter("https://app.example.com/shorts");

        assertThat(adapter.create(12L)).isEqualTo("https://app.example.com/shorts/12");
    }

    @Test
    void base_url_끝에_슬래시가_있으면_제거하고_붙인다() {
        ConfiguredShortsShareLinkAdapter adapter =
                new ConfiguredShortsShareLinkAdapter("https://app.example.com/shorts/");

        assertThat(adapter.create(12L)).isEqualTo("https://app.example.com/shorts/12");
    }
}
