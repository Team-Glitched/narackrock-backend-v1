package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.user.relation.port.out.ChatMessagePort.UnreadBySender;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ChatMessagePersistenceAdapterTest {
    @Autowired SpringDataDirectChatMessageRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void latestConversationsKeepOnlyTheNewestMessagePerPartner() {
        jdbc.update("INSERT INTO direct_chat_messages (id,sender_id,recipient_id,content,created_at) VALUES "
                + "(1,1,2,'old',CURRENT_TIMESTAMP),"
                + "(2,3,1,'other',CURRENT_TIMESTAMP),"
                + "(3,2,1,'new',CURRENT_TIMESTAMP),"
                + "(4,4,5,'unrelated',CURRENT_TIMESTAMP),"
                + "(5,1,3,'latest',CURRENT_TIMESTAMP)");

        var messages = new ChatMessagePersistenceAdapter(repository).latestConversations(1L, 10);

        assertThat(messages).extracting(message -> message.id()).containsExactly(5L, 3L);
        assertThat(messages).extracting(message -> message.content()).containsExactly("latest", "new");
    }

    @Test
    void countsIncomingMessagesAndMarksOnlyTheSelectedConversationThroughTheReadMessage() {
        jdbc.update("INSERT INTO direct_chat_messages (id,sender_id,recipient_id,content,created_at) "
                + "VALUES (100,2,1,'before unread tracking',CURRENT_TIMESTAMP)");
        var adapter = new ChatMessagePersistenceAdapter(repository);
        assertThat(adapter.unreadBySender(1L)).isEmpty();

        var first = adapter.save(2L, 1L, "first incoming");
        adapter.save(3L, 1L, "another conversation");
        adapter.save(1L, 2L, "outgoing");
        adapter.save(2L, 1L, "newer incoming");

        assertThat(adapter.unreadBySender(1L)).containsExactlyInAnyOrder(
                new UnreadBySender(2L, 2), new UnreadBySender(3L, 1));
        adapter.markRead(1L, 2L, first.id());
        adapter.markRead(1L, 2L, first.id());

        assertThat(adapter.unreadBySender(1L)).containsExactlyInAnyOrder(
                new UnreadBySender(2L, 1), new UnreadBySender(3L, 1));
        assertThat(adapter.unreadBySender(2L)).containsExactly(new UnreadBySender(1L, 1));
    }

    @Test
    void editUpdatesContentAndSoftDeleteClearsContentAndUnreadCount() {
        var adapter = new ChatMessagePersistenceAdapter(repository);
        var saved = adapter.save(2L, 1L, "initial");
        assertThat(adapter.unreadBySender(1L)).containsExactly(new UnreadBySender(2L, 1));

        LocalDateTime editTime = LocalDateTime.of(2026, 11, 24, 1, 5);
        var edited = adapter.updateContent(saved.id(), "edited text", editTime).orElseThrow();
        assertThat(edited.content()).isEqualTo("edited text");
        assertThat(edited.editedAt()).isEqualTo(editTime);
        assertThat(edited.deleted()).isFalse();

        LocalDateTime deleteTime = LocalDateTime.of(2026, 11, 24, 1, 10);
        var deleted = adapter.softDelete(saved.id(), deleteTime);
        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.content()).isEmpty();
        assertThat(adapter.unreadBySender(1L)).isEmpty();
        assertThat(adapter.findById(saved.id())).get().satisfies(found -> {
            assertThat(found.deleted()).isTrue();
            assertThat(found.content()).isEmpty();
        });
    }

    @Test
    void updateContentReturnsEmptyAfterSoftDeleteWithoutRevertingDeletion() {
        var adapter = new ChatMessagePersistenceAdapter(repository);
        var saved = adapter.save(2L, 1L, "initial");
        adapter.softDelete(saved.id(), LocalDateTime.of(2026, 11, 24, 1, 10));

        var lateEdit = adapter.updateContent(saved.id(), "resurrect?", LocalDateTime.of(2026, 11, 24, 1, 11));

        assertThat(lateEdit).isEmpty();
        assertThat(adapter.findById(saved.id())).get().satisfies(found -> {
            assertThat(found.deleted()).isTrue();
            assertThat(found.content()).isEmpty();
        });
        assertThat(adapter.unreadBySender(1L)).isEmpty();
    }

    @Test
    void updateContentPreservesReadStatusMarkedByMarkRead() {
        var adapter = new ChatMessagePersistenceAdapter(repository);
        var saved = adapter.save(2L, 1L, "initial");
        adapter.markRead(1L, 2L, saved.id());
        assertThat(adapter.unreadBySender(1L)).isEmpty();

        var edited = adapter.updateContent(saved.id(), "edited after read", LocalDateTime.of(2026, 11, 24, 1, 5))
                .orElseThrow();

        assertThat(edited.content()).isEqualTo("edited after read");
        assertThat(adapter.unreadBySender(1L)).isEmpty();
    }

    @Test
    void committedEditsAndDeletionIncrementRevisionRegardlessOfEditTimestamps() {
        jdbc.update("INSERT INTO direct_chat_messages (id,sender_id,recipient_id,content,created_at) "
                + "VALUES (200,2,1,'legacy',CURRENT_TIMESTAMP)");
        var adapter = new ChatMessagePersistenceAdapter(repository);
        assertThat(adapter.findById(200L)).get().extracting(message -> message.revision()).isEqualTo(0L);
        var saved = adapter.save(2L, 1L, "initial");
        assertThat(saved.revision()).isZero();

        // Edit B is committed first with the later timestamp; edit A commits afterwards with an earlier one.
        var editB = adapter.updateContent(saved.id(), "B", LocalDateTime.of(2026, 11, 24, 1, 6)).orElseThrow();
        var editA = adapter.updateContent(saved.id(), "A", LocalDateTime.of(2026, 11, 24, 1, 5)).orElseThrow();

        assertThat(editB.revision()).isEqualTo(1L);
        assertThat(editA.revision()).isEqualTo(2L);
        assertThat(adapter.findById(saved.id())).get().satisfies(found -> {
            assertThat(found.content()).isEqualTo("A");
            assertThat(found.revision()).isEqualTo(2L);
        });

        var deleted = adapter.softDelete(saved.id(), LocalDateTime.of(2026, 11, 24, 1, 10));
        assertThat(deleted.revision()).isEqualTo(3L);
        assertThat(adapter.updateContent(saved.id(), "late", LocalDateTime.of(2026, 11, 24, 1, 11))).isEmpty();
        assertThat(adapter.softDelete(saved.id(), LocalDateTime.of(2026, 11, 24, 1, 12)).revision()).isEqualTo(3L);
    }
}
