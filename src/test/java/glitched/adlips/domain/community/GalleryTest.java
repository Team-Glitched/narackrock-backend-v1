package glitched.adlips.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GalleryTest {

    @Test
    void 생성하면_이름과_설명을_다듬어_보관한다() {
        Gallery gallery = Gallery.create("  재즈  ", "  재즈 이야기  ");

        assertThat(gallery.getName()).isEqualTo("재즈");
        assertThat(gallery.getDescription()).isEqualTo("재즈 이야기");
    }

    @Test
    void 이름_규칙을_어기면_생성할_수_없다() {
        assertThatThrownBy(() -> Gallery.create("재", "설명")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gallery.create(" ", "설명")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 설명을_변경한다() {
        Gallery gallery = Gallery.create("재즈", "재즈 이야기");

        gallery.changeDescription("  새 소개  ");

        assertThat(gallery.getDescription()).isEqualTo("새 소개");
    }

    @Test
    void 설명이_비었거나_500자를_넘으면_변경할_수_없다() {
        Gallery gallery = Gallery.create("재즈", "재즈 이야기");

        assertThatThrownBy(() -> gallery.changeDescription("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gallery.changeDescription("가".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gallery.getDescription()).isEqualTo("재즈 이야기");
    }
}
