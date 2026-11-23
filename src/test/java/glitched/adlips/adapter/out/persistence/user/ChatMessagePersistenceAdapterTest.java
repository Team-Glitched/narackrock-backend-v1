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
        var edited = adapter.updateContent(saved.id(), "edited text", editTime);
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
}
