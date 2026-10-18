package glitched.adlips.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

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
}
