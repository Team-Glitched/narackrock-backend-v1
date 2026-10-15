package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import java.util.List;

public class DirectChatUseCase {
    private final FollowRepositoryPort follows;
    private final ChatMessagePort messages;
    private final TransactionRunner transactions;
    public DirectChatUseCase(FollowRepositoryPort follows, ChatMessagePort messages) { this(follows, messages, TransactionRunner.direct()); }
    public DirectChatUseCase(FollowRepositoryPort follows, ChatMessagePort messages, TransactionRunner transactions) { this.follows = follows; this.messages = messages; this.transactions = transactions; }
    public ChatMessagePort.Message send(Long senderId, Long recipientId, String content) {
        return transactions.required(() -> {
            requireMutual(senderId, recipientId);
            String message = content == null ? "" : content.trim();
            if (message.isEmpty() || message.length() > 2000) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "메시지는 1~2000자로 입력해 주세요.");
            return messages.save(senderId, recipientId, message);
        });
    }
    public List<ChatMessagePort.Message> conversation(Long viewerId, Long friendId, Long beforeId, int size) {
        return transactions.readOnly(() -> {
            requireMutual(viewerId, friendId);
            if (size < 1 || size > 100) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "조회 개수를 확인해 주세요.");
            return messages.conversation(viewerId, friendId, beforeId, size);
        });
    }
    private void requireMutual(Long a, Long b) {
        if (a == null || b == null || a.equals(b) || !follows.exists(a, b) || !follows.exists(b, a)) {
            throw new UserApplicationException(UserErrorCode.UNAUTHORIZED_ACCESS, "맞팔로우한 사용자끼리만 대화할 수 있습니다.");
        }
    }
}
