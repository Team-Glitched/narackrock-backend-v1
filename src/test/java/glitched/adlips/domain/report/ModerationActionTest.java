package glitched.adlips.domain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import org.junit.jupiter.api.Test;

class ModerationActionTest {

    @Test
    void 필드를_전달하면_그대로_보관한다() {
        User admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);
        User reporter = User.create("reporter@example.com").withId(1L);
        Report report = new Report(reporter, ReportTargetType.SHORT, 12L, "COPYRIGHT", "설명");

        ModerationAction action = new ModerationAction(
                report, admin, ReportTargetType.SHORT, 12L, ModerationActionType.HIDE_CONTENT, "운영 정책 위반");

        assertThat(action.getReport()).isEqualTo(report);
        assertThat(action.getAdmin()).isEqualTo(admin);
        assertThat(action.getTargetType()).isEqualTo(ReportTargetType.SHORT);
        assertThat(action.getTargetId()).isEqualTo(12L);
        assertThat(action.getActionType()).isEqualTo(ModerationActionType.HIDE_CONTENT);
        assertThat(action.getReason()).isEqualTo("운영 정책 위반");
    }

    @Test
    void report는_없어도_된다() {
        User admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);

        ModerationAction action = new ModerationAction(
                null, admin, ReportTargetType.SHORT, 12L, ModerationActionType.HIDE_CONTENT, "사유");

        assertThat(action.getReport()).isNull();
    }

    @Test
    void admin이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new ModerationAction(
                null, null, ReportTargetType.SHORT, 12L, ModerationActionType.HIDE_CONTENT, "사유"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void targetType이_없으면_예외가_발생한다() {
        User admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);

        assertThatThrownBy(() -> new ModerationAction(
                null, admin, null, 12L, ModerationActionType.HIDE_CONTENT, "사유"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void targetId가_없으면_예외가_발생한다() {
        User admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);

        assertThatThrownBy(() -> new ModerationAction(
                null, admin, ReportTargetType.SHORT, null, ModerationActionType.HIDE_CONTENT, "사유"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void actionType이_없으면_예외가_발생한다() {
        User admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);

        assertThatThrownBy(() -> new ModerationAction(null, admin, ReportTargetType.SHORT, 12L, null, "사유"))
                .isInstanceOf(NullPointerException.class);
    }
}
