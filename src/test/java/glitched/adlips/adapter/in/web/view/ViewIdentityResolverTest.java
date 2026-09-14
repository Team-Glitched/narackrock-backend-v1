package glitched.adlips.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.view.ContentViewApplicationException;
import glitched.adlips.application.view.ContentViewErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ViewIdentityResolverTest {

    @Mock AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    void 로그인_사용자는_사용자_ID를_우선한다() {
        when(authenticatedUserResolver.resolveOptionalUserId("Bearer token")).thenReturn(7L);
        ViewIdentityResolver resolver = new ViewIdentityResolver(authenticatedUserResolver);

        assertThat(resolver.resolve("Bearer token", "device-id")).isEqualTo("user:7");
    }

    @Test
    void 비로그인_사용자는_기기_ID를_해시해서_사용한다() {
        when(authenticatedUserResolver.resolveOptionalUserId(null)).thenReturn(null);
        ViewIdentityResolver resolver = new ViewIdentityResolver(authenticatedUserResolver);

        String identity = resolver.resolve(null, " device-id ");

        assertThat(identity).startsWith("device:");
        assertThat(identity).doesNotContain("device-id");
        assertThat(identity).hasSize(71);
    }

    @Test
    void 인증과_기기_ID가_모두_없으면_예외가_발생한다() {
        when(authenticatedUserResolver.resolveOptionalUserId(null)).thenReturn(null);
        ViewIdentityResolver resolver = new ViewIdentityResolver(authenticatedUserResolver);

        assertThatThrownBy(() -> resolver.resolve(null, " "))
                .isInstanceOf(ContentViewApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ContentViewErrorCode.VIEWER_ID_REQUIRED);
    }
}
