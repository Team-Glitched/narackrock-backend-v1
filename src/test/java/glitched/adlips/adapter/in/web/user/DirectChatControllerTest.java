package glitched.adlips.adapter.in.web.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import glitched.adlips.domain.user.Profile;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DirectChatControllerTest {
    @Test
    void recentThreadsExposeThePartnerLastMessageAndUnreadCount() {
        DirectChatUseCase chat = mock(DirectChatUseCase.class);
        AuthenticatedUserResolver users = mock(AuthenticatedUserResolver.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        LocalDateTime sentAt = LocalDateTime.of(2026, 10, 19, 9, 0);
        when(users.requireUserId("Bearer test")).thenReturn(1L);
        when(chat.latestConversations(1L)).thenReturn(List.of(
                new DirectChatUseCase.ThreadSummary(
                        2L, new ChatMessagePort.Message(5L, 2L, 1L, "hello", sentAt), 2L)));
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.create(2L, "friend")));

        var response = new DirectChatController(chat, users, mock(ChatRealtimePublisher.class), profiles, mediaFiles)
                .threads("Bearer test");

        assertThat(response.data()).singleElement().satisfies(thread -> {
            assertThat(thread.userId()).isEqualTo(2L);
            assertThat(thread.nickname()).isEqualTo("friend");
            assertThat(thread.lastMessageId()).isEqualTo(5L);
            assertThat(thread.lastMessage()).isEqualTo("hello");
            assertThat(thread.lastMessageAt()).isEqualTo(sentAt.atZone(ZoneId.systemDefault()).toInstant());
            assertThat(thread.unreadCount()).isEqualTo(2);
            assertThat(thread.outgoing()).isFalse();
        });
    }

    @Test
    void deletedLatestMessageShowsPlaceholderInThreadsAndEditDeletePublishRealtimeUpdates() {
        DirectChatUseCase chat = mock(DirectChatUseCase.class);
        AuthenticatedUserResolver users = mock(AuthenticatedUserResolver.class);
        ChatRealtimePublisher realtime = mock(ChatRealtimePublisher.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        MediaFileRepositoryPort mediaFiles = mock(MediaFileRepositoryPort.class);
        LocalDateTime sentAt = LocalDateTime.of(2026, 11, 24, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 24, 9, 5);
        when(users.requireUserId("Bearer test")).thenReturn(1L);
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(Profile.create(2L, "friend")));

        var edited = new ChatMessagePort.Message(7L, 1L, 2L, "updated", sentAt, updatedAt, false, 1L);
        var deleted = new ChatMessagePort.Message(7L, 1L, 2L, "", sentAt, updatedAt, true, 2L);
        when(chat.edit(1L, 2L, 7L, "updated")).thenReturn(edited);
        when(chat.delete(1L, 2L, 7L)).thenReturn(deleted);
        when(chat.latestConversations(1L)).thenReturn(List.of(
                new DirectChatUseCase.ThreadSummary(2L, deleted, 0L)));

        var controller = new DirectChatController(chat, users, realtime, profiles, mediaFiles);

        var editResponse = controller.edit("Bearer test", 2L, 7L, new DirectChatController.EditMessageRequest("updated"));
        assertThat(editResponse.data().content()).isEqualTo("updated");
        assertThat(editResponse.data().editedAt()).isEqualTo(updatedAt);
        assertThat(editResponse.data().revision()).isEqualTo(1L);
        verify(realtime).publish(edited);

        var deleteResponse = controller.delete("Bearer test", 2L, 7L);
        assertThat(deleteResponse.data().deleted()).isTrue();
        assertThat(deleteResponse.data().content()).isEmpty();
        assertThat(deleteResponse.data().revision()).isEqualTo(2L);
        verify(realtime).publish(deleted);

        var threadsResponse = controller.threads("Bearer test");
        assertThat(threadsResponse.data()).singleElement().satisfies(thread ->
                assertThat(thread.lastMessage()).isEqualTo("삭제된 메시지입니다."));
    }
}
