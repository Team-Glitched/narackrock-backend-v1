package glitched.adlips.adapter.out.community;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfiguredPostShareLinkAdapterTest {

    @Test
    void base_url_끝에_슬래시가_없으면_그대로_붙인다() {
        ConfiguredPostShareLinkAdapter adapter = new ConfiguredPostShareLinkAdapter("https://app.example.com/posts");

        assertThat(adapter.create(501L)).isEqualTo("https://app.example.com/posts/501");
    }

    @Test
    void base_url_끝에_슬래시가_있으면_제거하고_붙인다() {
        ConfiguredPostShareLinkAdapter adapter = new ConfiguredPostShareLinkAdapter("https://app.example.com/posts/");

        assertThat(adapter.create(501L)).isEqualTo("https://app.example.com/posts/501");
    }
}
