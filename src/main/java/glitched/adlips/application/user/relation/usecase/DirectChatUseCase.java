package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.common.TextSanitizer;
import glitched.adlips.domain.user.Profile;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class DirectChatUseCase {
    public record ThreadSummary(Long partnerId, ChatMessagePort.Message lastMessage, long unreadCount) {}

    private final FollowRepositoryPort follows;
    private final ProfileRepositoryPort profiles;
    private final ChatMessagePort messages;
    private final TransactionRunner transactions;
    private final Clock clock;
    public DirectChatUseCase(FollowRepositoryPort follows, ProfileRepositoryPort profiles, ChatMessagePort messages) { this(follows, profiles, messages, TransactionRunner.direct()); }
    public DirectChatUseCase(FollowRepositoryPort follows, ProfileRepositoryPort profiles, ChatMessagePort messages, TransactionRunner transactions) { this(follows, profiles, messages, transactions, Clock.systemDefaultZone()); }
    public DirectChatUseCase(FollowRepositoryPort follows, ProfileRepositoryPort profiles, ChatMessagePort messages, TransactionRunner transactions, Clock clock) { this.follows = follows; this.profiles = profiles; this.messages = messages; this.transactions = transactions; this.clock = clock; }
    public ChatMessagePort.Message send(Long senderId, Long recipientId, String content) {
        return transactions.required(() -> {
            requireChatAccess(senderId, recipientId);
            String message = validateMessageContent(content);
            return messages.save(senderId, recipientId, message);
        });
    }
    public ChatMessagePort.Message edit(Long userId, Long friendId, Long messageId, String content) {
        if (messageId == null || messageId < 1) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "메시지를 확인해 주세요.");
        }
        return transactions.required(() -> {
            requireConversationAccess(userId, friendId);
            ChatMessagePort.Message existing = requireConversationMessage(userId, friendId, messageId);
            if (!userId.equals(existing.senderId())) {
                throw new UserApplicationException(UserErrorCode.CHAT_FORBIDDEN, "본인이 보낸 메시지만 수정할 수 있습니다.");
            }
            if (existing.deleted()) {
                throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "삭제된 메시지는 수정할 수 없습니다.");
            }
            String message = validateMessageContent(content);
            return messages.updateContent(messageId, message, LocalDateTime.now(clock));
        });
    }
    public ChatMessagePort.Message delete(Long userId, Long friendId, Long messageId) {
        if (messageId == null || messageId < 1) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "메시지를 확인해 주세요.");
        }
        return transactions.required(() -> {
            requireConversationAccess(userId, friendId);
            ChatMessagePort.Message existing = requireConversationMessage(userId, friendId, messageId);
            if (!userId.equals(existing.senderId())) {
                throw new UserApplicationException(UserErrorCode.CHAT_FORBIDDEN, "본인이 보낸 메시지만 삭제할 수 있습니다.");
            }
            if (existing.deleted()) {
                return existing;
            }
            return messages.softDelete(messageId, LocalDateTime.now(clock));
        });
    }
    private ChatMessagePort.Message requireConversationMessage(Long userId, Long friendId, Long messageId) {
        ChatMessagePort.Message existing = messages.findById(messageId)
                .orElseThrow(() -> new UserApplicationException(UserErrorCode.CHAT_MESSAGE_NOT_FOUND, "메시지를 찾을 수 없습니다."));
        boolean matchesConversation = (userId.equals(existing.senderId()) && friendId.equals(existing.recipientId()))
                || (friendId.equals(existing.senderId()) && userId.equals(existing.recipientId()));
        if (!matchesConversation) {
            throw new UserApplicationException(UserErrorCode.CHAT_MESSAGE_NOT_FOUND, "메시지를 찾을 수 없습니다.");
        }
        return existing;
    }
    private String validateMessageContent(String content) {
        String message = TextSanitizer.sanitizeMultiline(content);
        if (message.isEmpty() || ContentLimits.exceeds(message, ContentLimits.CHAT_MESSAGE_MAX_LENGTH))
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "메시지는 1~2000자로 입력해 주세요.");
        return message;
    }
    public List<ChatMessagePort.Message> conversation(Long viewerId, Long friendId, Long beforeId, int size) {
        return transactions.readOnly(() -> {
            requireConversationAccess(viewerId, friendId);
            if (size < 1 || size > 100) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "조회 개수를 확인해 주세요.");
            return messages.conversation(viewerId, friendId, beforeId, size);
        });
    }
    public List<ThreadSummary> latestConversations(Long viewerId) {
        if (viewerId == null) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "사용자를 확인해 주세요.");
        return transactions.readOnly(() -> {
            var latest = messages.latestConversations(viewerId, 100);
            if (latest.isEmpty()) return List.of();
            Profile viewer = profiles.findByUserId(viewerId).orElseThrow(() ->
                    new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
            var unreadBySender = messages.unreadBySender(viewerId).stream()
                    .collect(Collectors.toMap(ChatMessagePort.UnreadBySender::senderId, ChatMessagePort.UnreadBySender::count));
            return latest.stream().map(message -> {
                Long partnerId = viewerId.equals(message.senderId()) ? message.recipientId() : message.senderId();
                return new ThreadSummary(partnerId, message, unreadBySender.getOrDefault(partnerId, 0L));
            }).filter(thread -> canReadConversationWith(viewer, viewerId, thread.partnerId())).toList();
        });
    }

    public long unreadCount(Long viewerId) {
        return transactions.readOnly(() -> {
            var unread = messages.unreadBySender(viewerId);
            if (unread.isEmpty()) return 0L;
            Profile viewer = profiles.findByUserId(viewerId).orElseThrow(() ->
                    new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
            return unread.stream()
                    .filter(item -> canReadConversationWith(viewer, viewerId, item.senderId()))
                    .mapToLong(ChatMessagePort.UnreadBySender::count)
                    .sum();
        });
    }

    public void markRead(Long viewerId, Long friendId, Long throughId) {
        if (throughId == null || throughId < 1) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "읽은 메시지를 확인해 주세요.");
        }
        transactions.required(() -> {
            requireConversationAccess(viewerId, friendId);
            messages.markRead(viewerId, friendId, throughId);
            return null;
        });
    }

    private void requireChatAccess(Long a, Long b) {
        if (a == null || b == null || a.equals(b)) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "다른 사용자와의 대화만 가능합니다.");
        }
        Profile recipient = profiles.findByUserId(b).orElseThrow(() -> new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
        if (recipient.isPrivate() && (!follows.exists(a, b) || !follows.exists(b, a))) {
            throw new UserApplicationException(UserErrorCode.CHAT_FORBIDDEN, "비공개 계정과는 맞팔로우한 경우에만 대화할 수 있습니다.");
        }
    }

    private void requireConversationAccess(Long viewerId, Long friendId) {
        if (viewerId == null || friendId == null || viewerId.equals(friendId)) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "다른 사용자와의 대화만 가능합니다.");
        }
        Profile viewer = profiles.findByUserId(viewerId).orElseThrow(() -> new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
        Profile friend = profiles.findByUserId(friendId).orElseThrow(() -> new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
        if (!canReadConversation(viewer, friend, viewerId, friendId)) {
            throw new UserApplicationException(UserErrorCode.CHAT_FORBIDDEN, "비공개 계정과는 맞팔로우한 경우에만 대화할 수 있습니다.");
        }
    }

    private boolean canReadConversation(Profile viewer, Profile friend, Long viewerId, Long friendId) {
        return !viewer.isPrivate() || !friend.isPrivate()
                || (follows.exists(viewerId, friendId) && follows.exists(friendId, viewerId));
    }

    private boolean canReadConversationWith(Profile viewer, Long viewerId, Long partnerId) {
        return profiles.findByUserId(partnerId)
                .map(partner -> canReadConversation(viewer, partner, viewerId, partnerId))
                .orElse(false);
    }
}
