package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.TrackVolumeUpdateRequest;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.dto.response.TrackVolumeUpdateResponse;
import glitched.adlips.application.project.usecase.TrackVolumeUpdateUseCase;
import org.junit.jupiter.api.Test;

class TrackVolumeControllerTest {
    @Test
    void returnsUpdatedTrackVolume() {
        TrackVolumeUpdateUseCase useCase = mock(TrackVolumeUpdateUseCase.class);
        AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireUserId("Bearer token")).thenReturn(1L);
        when(useCase.execute(any())).thenReturn(new TrackVolumeUpdateResponse(
                101L, 75, null, new ProjectVersionResponse(1, 2, "v1.2")));

        ApiResponse<TrackVolumeUpdateResponse> result = new TrackVolumeController(useCase, resolver)
                .update(101L, "Bearer token", new TrackVolumeUpdateRequest(null, null, 75));

        assertThat(result.message()).isEqualTo("트랙 볼륨이 조절되었습니다.");
        assertThat(result.data().volume()).isEqualTo(75);
    }
}
