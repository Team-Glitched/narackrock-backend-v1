package glitched.adlips.application.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.usecase.ProfileMuteToggleUseCase;
import glitched.adlips.domain.user.Profile;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProfileMuteToggleUseCaseTest {

    @Test
    void 처음_호출하면_음소거_상태로_바뀐다() {
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        Profile profile = Profile.create(1L, "guitar_moon");
        when(profiles.findByUserIdForUpdate(1L)).thenReturn(Optional.of(profile));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProfileMuteToggleUseCase useCase = new ProfileMuteToggleUseCase(profiles);

        boolean result = useCase.execute(1L);

        assertThat(result).isTrue();
    }

    @Test
    void 이미_음소거_상태면_다시_호출시_해제된다() {
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        Profile profile = Profile.create(1L, "guitar_moon");
        profile.toggleMuted();
        when(profiles.findByUserIdForUpdate(1L)).thenReturn(Optional.of(profile));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProfileMuteToggleUseCase useCase = new ProfileMuteToggleUseCase(profiles);

        boolean result = useCase.execute(1L);

        assertThat(result).isFalse();
    }

    @Test
    void 프로필이_없으면_PROFILE_NOT_FOUND_예외가_발생한다() {
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        when(profiles.findByUserIdForUpdate(999L)).thenReturn(Optional.empty());
        ProfileMuteToggleUseCase useCase = new ProfileMuteToggleUseCase(profiles);

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(UserApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.PROFILE_NOT_FOUND);
    }
}
