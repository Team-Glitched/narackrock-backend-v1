package glitched.adlips.domain.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;

class ShortReportTest {

    @Test
    void 필드를_전달하면_그대로_보관한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        ShortForm shorts = mockShortForm();

        ShortReport report = new ShortReport(reporter, shorts, "COPYRIGHT", "저작권 침해가 의심됩니다.");

        assertThat(report.getId()).isNull();
    }

    @Test
    void reporter가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new ShortReport(null, mockShortForm(), "COPYRIGHT", "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shorts가_없으면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        assertThatThrownBy(() -> new ShortReport(reporter, null, "COPYRIGHT", "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reason이_없으면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        assertThatThrownBy(() -> new ShortReport(reporter, mockShortForm(), null, "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    private ShortForm mockShortForm() {
        return org.mockito.Mockito.mock(ShortForm.class);
    }
}
