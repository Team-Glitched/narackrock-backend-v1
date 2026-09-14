package glitched.adlips.domain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import java.time.LocalDateTime;
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

    @Test
    void reason이_공백이면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);

        assertThatThrownBy(() -> new Report(reporter, ReportTargetType.SHORT, 12L, "  ", "설명"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolve_호출시_상태와_처리자와_처리시각이_반영된다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        User admin = User.restore(2L, "admin@example.com", glitched.adlips.domain.user.UserRole.ADMIN, null);
        Report report = new Report(reporter, ReportTargetType.SHORT, 12L, "COPYRIGHT", "설명");
        LocalDateTime handledAt = LocalDateTime.of(2026, 7, 8, 14, 40);

        report.resolve(admin, ReportStatus.RESOLVED, handledAt);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getHandledAt()).isEqualTo(handledAt);
    }

    @Test
    void PENDING이_아닌_상태에서_resolve를_다시_호출하면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        User admin = User.restore(2L, "admin@example.com", glitched.adlips.domain.user.UserRole.ADMIN, null);
        Report report = new Report(reporter, ReportTargetType.SHORT, 12L, "COPYRIGHT", "설명");
        report.resolve(admin, ReportStatus.RESOLVED, LocalDateTime.now());

        assertThatThrownBy(() -> report.resolve(admin, ReportStatus.REJECTED, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolve의_admin이_없으면_예외가_발생한다() {
        User reporter = User.create("reporter@example.com").withId(1L);
        Report report = new Report(reporter, ReportTargetType.SHORT, 12L, "COPYRIGHT", "설명");

        assertThatThrownBy(() -> report.resolve(null, ReportStatus.RESOLVED, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
    }
}
