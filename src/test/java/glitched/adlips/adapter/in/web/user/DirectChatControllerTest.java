package glitched.adlips.adapter.in.web.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import glitched.adlips.domain.user.Profile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DirectChatControllerTest {
    @Test
    void recentThreadsExposeThePartnerAndLastMessage() {
        DirectChatUseCase chat = mock(DirectChatUseCase.class);
        AuthenticatedUserResolver users = mock(AuthenticatedUserResolver.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        LocalDateTime sentAt = LocalDateTime.of(2026, 10, 19, 9, 0);
        when(users.requireUserId("Bearer test")).thenReturn(1L);
        when(chat.latestConversations(1L)).thenReturn(List.of(
                new ChatMessagePort.Message(5L, 2L, 1L, "hello", sentAt)));
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.create(2L, "friend")));

        var response = new DirectChatController(chat, users, mock(ChatRealtimePublisher.class), profiles, mediaFiles)
                .threads("Bearer test");

        assertThat(response.data()).singleElement().satisfies(thread -> {
            assertThat(thread.userId()).isEqualTo(2L);
            assertThat(thread.nickname()).isEqualTo("friend");
            assertThat(thread.lastMessageId()).isEqualTo(5L);
            assertThat(thread.lastMessage()).isEqualTo("hello");
            assertThat(thread.lastMessageAt()).isEqualTo(sentAt);
            assertThat(thread.outgoing()).isFalse();
        });
    }
}
