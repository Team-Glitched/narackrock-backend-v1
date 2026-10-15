package glitched.adlips.application.user.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.relation.port.out.FollowRequestPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.usecase.FollowRequestUseCase;
import glitched.adlips.application.user.relation.usecase.FollowCreateUseCase;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FollowRequestUseCaseTest {
    private final FollowRequestPort requests = mock(FollowRequestPort.class);
    private final FollowRepositoryPort follows = mock(FollowRepositoryPort.class);
    private final FollowCreateUseCase create = mock(FollowCreateUseCase.class);
    private final ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
    private final FollowRequestUseCase useCase = new FollowRequestUseCase(requests, follows, profiles, create);

    @Test
    void privateProfileRequiresApproval() {
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.restore(2L, "private", null, null, null, true, 0, 0, null)));
        when(requests.create(1L, 2L)).thenReturn(10L);
        assertEquals(10L, useCase.follow(1L, 2L).requestId());
        verifyNoInteractions(create);
        when(requests.find(10L)).thenReturn(new FollowRequestPort.Request(10L, 1L, 2L));
        useCase.accept(2L, 10L);
        verify(create).execute(new glitched.adlips.application.user.relation.dto.request.FollowCreateRequest(1L, 2L));
        verify(requests).delete(10L);
    }

    @Test
    void publicProfileFollowsImmediately() {
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.restore(2L, "public", null, null, null, false, 0, 0, null)));
        assertEquals("FOLLOWING", useCase.follow(1L, 2L).status());
        verify(requests).exists(1L, 2L);
        verifyNoMoreInteractions(requests);
        verify(create).execute(new glitched.adlips.application.user.relation.dto.request.FollowCreateRequest(1L, 2L));
    }

    @Test
    void anotherUserCannotAcceptRequest() {
        when(requests.find(10L)).thenReturn(new FollowRequestPort.Request(10L, 1L, 2L));
        assertThrows(UserApplicationException.class, () -> useCase.accept(3L, 10L));
        verifyNoInteractions(create);
    }
}
