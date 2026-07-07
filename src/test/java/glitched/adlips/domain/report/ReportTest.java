package glitched.adlips.domain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;

class ReportTest {

    @Test
    void 필드를_전달하면_그대로_보관하고_기본_상태는_PENDING이다() {
        User reporter = User.create("reporter@example.com").withId(1L);

        Report report = new Report(reporter, ReportTargetType.SHORT, 12L, "COPYRIGHT", "저작권 침해가 의심됩니다.");

        assertThat(report.getId()).isNull();
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.SHORT);
        assertThat(report.getTargetId()).isEqualTo(12L);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void reporter가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new Report(null, ReportTargetType.SHORT, 12L, "COPYRIGHT", "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void targetType이_없으면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        assertThatThrownBy(() -> new Report(reporter, null, 12L, "COPYRIGHT", "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void targetId가_없으면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        assertThatThrownBy(() -> new Report(reporter, ReportTargetType.SHORT, null, "COPYRIGHT", "설명"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reason이_없으면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        assertThatThrownBy(() -> new Report(reporter, ReportTargetType.SHORT, 12L, null, "설명"))
                .isInstanceOf(NullPointerException.class);
    }
}
