package glitched.adlips.application.user.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import glitched.adlips.domain.user.Profile;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DirectChatUseCaseTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-24T01:00:00Z"), ZoneOffset.UTC);
    private final FollowRepositoryPort follows = mock(FollowRepositoryPort.class);
    private final ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
    private final ChatMessagePort messages = mock(ChatMessagePort.class);
    private final DirectChatUseCase chat = new DirectChatUseCase(
            follows, profiles, messages, TransactionRunner.direct(), FIXED_CLOCK);

    private void privacy(boolean senderPrivate, boolean recipientPrivate) {
        Profile sender = mock(Profile.class);
        Profile recipient = mock(Profile.class);
        when(sender.isPrivate()).thenReturn(senderPrivate);
        when(recipient.isPrivate()).thenReturn(recipientPrivate);
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(recipient));
    }

    @Test
    void privateRecipientRequiresMutualFollowAndReturnsForbidden() {
        privacy(false, true);
        when(follows.exists(1L, 2L)).thenReturn(true);
        UserApplicationException error = assertThrows(UserApplicationException.class, () -> chat.send(1L, 2L, "hello"));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(messages);
    }

    @Test
    void publicRecipientCanReceiveWithoutFollow() {
        privacy(false, false);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
        chat.conversation(1L, 2L, null, 50);
        verify(messages).conversation(1L, 2L, null, 50);
    }

    @Test
    void privateSenderCanMessagePublicRecipientWithoutFollow() {
        privacy(true, false);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
    }

    @Test
    void publicRecipientCanReadMessagesFromPrivateSender() {
        privacy(true, false);
        chat.conversation(2L, 1L, null, 50);
        verify(messages).conversation(2L, 1L, null, 50);
    }

    @Test
    void privateRecipientCanChatWhenBothDirectionsFollow() {
        privacy(false, true);
        when(follows.exists(1L, 2L)).thenReturn(true);
        when(follows.exists(2L, 1L)).thenReturn(true);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
    }

    @Test
    void privateConversationCannotBeReadAfterUnfollow() {
        privacy(true, true);
        when(follows.exists(1L, 2L)).thenReturn(true);

        UserApplicationException error = assertThrows(UserApplicationException.class,
                () -> chat.conversation(1L, 2L, null, 50));

        assertEquals(UserErrorCode.CHAT_FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(messages);
    }

    @Test
    void privateSenderCannotMessagePrivateRecipientWithoutMutualFollow() {
        privacy(true, true);
        UserApplicationException error = assertThrows(UserApplicationException.class,
                () -> chat.send(1L, 2L, "hello"));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(messages);
    }

    @Test
    void inaccessiblePrivateConversationIsExcludedFromInbox() {
        privacy(true, true);
        var latest = new ChatMessagePort.Message(5L, 1L, 2L, "old message", LocalDateTime.now());
        when(messages.latestConversations(1L, 100)).thenReturn(List.of(latest));

        assertEquals(List.of(), chat.latestConversations(1L));
    }

    @Test
    void nonMutualPublicConversationRemainsInInbox() {
        privacy(true, false);
        var latest = new ChatMessagePort.Message(5L, 1L, 2L, "old message", LocalDateTime.now());
        when(messages.latestConversations(1L, 100)).thenReturn(List.of(latest));

        assertEquals(List.of(new DirectChatUseCase.ThreadSummary(2L, latest, 0)), chat.latestConversations(1L));
    }

    @Test
    void mutualPrivateConversationRemainsInInbox() {
        privacy(true, true);
        when(follows.exists(1L, 2L)).thenReturn(true);
        when(follows.exists(2L, 1L)).thenReturn(true);
        var latest = new ChatMessagePort.Message(5L, 1L, 2L, "old message", LocalDateTime.now());
        when(messages.latestConversations(1L, 100)).thenReturn(List.of(latest));

        assertEquals(List.of(new DirectChatUseCase.ThreadSummary(2L, latest, 0)), chat.latestConversations(1L));
    }

    @Test
    void threadKeepsUnreadIncomingCountWhenItsLatestMessageIsOutgoing() {
        privacy(false, false);
        var latest = new ChatMessagePort.Message(5L, 1L, 2L, "outgoing", LocalDateTime.now());
        when(messages.latestConversations(1L, 100)).thenReturn(List.of(latest));
        when(messages.unreadBySender(1L)).thenReturn(List.of(new ChatMessagePort.UnreadBySender(2L, 3)));

        assertEquals(List.of(new DirectChatUseCase.ThreadSummary(2L, latest, 3)), chat.latestConversations(1L));
    }

    @Test
    void unreadCountIncludesOnlyReadableConversations() {
        privacy(true, true);
        Profile publicSender = mock(Profile.class);
        when(profiles.findByUserId(3L)).thenReturn(Optional.of(publicSender));
        when(messages.unreadBySender(1L)).thenReturn(List.of(
                new ChatMessagePort.UnreadBySender(2L, 2), new ChatMessagePort.UnreadBySender(3L, 1)));

        assertEquals(1L, chat.unreadCount(1L));

        when(follows.exists(1L, 2L)).thenReturn(true);
        when(follows.exists(2L, 1L)).thenReturn(true);
        assertEquals(3L, chat.unreadCount(1L));
    }

    @Test
    void onlyReadableConversationsCanBeMarkedRead() {
        privacy(true, true);
        UserApplicationException denied = assertThrows(UserApplicationException.class,
                () -> chat.markRead(1L, 2L, 5L));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, denied.getErrorCode());
        verifyNoInteractions(messages);

        when(follows.exists(1L, 2L)).thenReturn(true);
        when(follows.exists(2L, 1L)).thenReturn(true);
        chat.markRead(1L, 2L, 5L);

        verify(messages).markRead(1L, 2L, 5L);
    }

    @Test
    void senderCanEditAndSoftDeleteOwnMessageWithSanitizationAndLimits() {
        privacy(false, false);
        LocalDateTime sentAt = LocalDateTime.of(2026, 11, 24, 0, 0);
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        var original = new ChatMessagePort.Message(10L, 1L, 2L, "hello", sentAt);
        var updated = new ChatMessagePort.Message(10L, 1L, 2L, "edited!", sentAt, now, false);
        var deleted = new ChatMessagePort.Message(10L, 1L, 2L, "", sentAt, now, true);
        when(messages.findById(10L)).thenReturn(Optional.of(original));
        when(messages.updateContent(10L, "edited!", now)).thenReturn(updated);
        when(messages.softDelete(10L, now)).thenReturn(deleted);

        var editedResult = chat.edit(1L, 2L, 10L, "\u3164edited!\u200B");
        assertEquals("edited!", editedResult.content());
        assertEquals(now, editedResult.editedAt());

        assertThrows(UserApplicationException.class, () -> chat.edit(1L, 2L, 10L, "\u3164\u2800"));
        assertThrows(UserApplicationException.class, () -> chat.edit(1L, 2L, 10L, "a".repeat(2001)));

        var deletedResult = chat.delete(1L, 2L, 10L);
        assertTrue(deletedResult.deleted());
        assertEquals("", deletedResult.content());

        // Idempotent delete and rejected edit on already deleted message
        when(messages.findById(10L)).thenReturn(Optional.of(deleted));
        assertSame(deleted, chat.delete(1L, 2L, 10L));
        verify(messages, times(1)).softDelete(10L, now);

        UserApplicationException editDeleted = assertThrows(UserApplicationException.class,
                () -> chat.edit(1L, 2L, 10L, "retry"));
        assertEquals(UserErrorCode.VALIDATION_ERROR, editDeleted.getErrorCode());
    }

    @Test
    void nonSenderOrWrongConversationCannotEditOrDelete() {
        privacy(false, false);
        var sentByFriend = new ChatMessagePort.Message(11L, 2L, 1L, "theirs", LocalDateTime.now(FIXED_CLOCK));
        var wrongConversation = new ChatMessagePort.Message(12L, 1L, 3L, "other", LocalDateTime.now(FIXED_CLOCK));
        when(messages.findById(11L)).thenReturn(Optional.of(sentByFriend));
        when(messages.findById(12L)).thenReturn(Optional.of(wrongConversation));
        when(messages.findById(99L)).thenReturn(Optional.empty());

        UserApplicationException forbiddenEdit = assertThrows(UserApplicationException.class,
                () -> chat.edit(1L, 2L, 11L, "hack"));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, forbiddenEdit.getErrorCode());

        UserApplicationException forbiddenDelete = assertThrows(UserApplicationException.class,
                () -> chat.delete(1L, 2L, 11L));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, forbiddenDelete.getErrorCode());

        UserApplicationException wrongConv = assertThrows(UserApplicationException.class,
                () -> chat.edit(1L, 2L, 12L, "hack"));
        assertEquals(UserErrorCode.CHAT_MESSAGE_NOT_FOUND, wrongConv.getErrorCode());

        UserApplicationException missing = assertThrows(UserApplicationException.class,
                () -> chat.delete(1L, 2L, 99L));
        assertEquals(UserErrorCode.CHAT_MESSAGE_NOT_FOUND, missing.getErrorCode());
    }
}
