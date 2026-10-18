package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.domain.user.Profile;
import java.util.List;

public class DirectChatUseCase {
    private final FollowRepositoryPort follows;
    private final ProfileRepositoryPort profiles;
    private final ChatMessagePort messages;
    private final TransactionRunner transactions;
    public DirectChatUseCase(FollowRepositoryPort follows, ProfileRepositoryPort profiles, ChatMessagePort messages) { this(follows, profiles, messages, TransactionRunner.direct()); }
    public DirectChatUseCase(FollowRepositoryPort follows, ProfileRepositoryPort profiles, ChatMessagePort messages, TransactionRunner transactions) { this.follows = follows; this.profiles = profiles; this.messages = messages; this.transactions = transactions; }
    public ChatMessagePort.Message send(Long senderId, Long recipientId, String content) {
        return transactions.required(() -> {
            requireChatAccess(senderId, recipientId);
            String message = content == null ? "" : content.trim();
            if (message.isEmpty() || message.length() > 2000) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "메시지는 1~2000자로 입력해 주세요.");
            return messages.save(senderId, recipientId, message);
        });
    }
    public List<ChatMessagePort.Message> conversation(Long viewerId, Long friendId, Long beforeId, int size) {
        return transactions.readOnly(() -> {
            requireConversationAccess(viewerId, friendId);
            if (size < 1 || size > 100) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "조회 개수를 확인해 주세요.");
            return messages.conversation(viewerId, friendId, beforeId, size);
        });
    }
    public List<ChatMessagePort.Message> latestConversations(Long viewerId) {
        if (viewerId == null) throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "사용자를 확인해 주세요.");
        return transactions.readOnly(() -> {
            var latest = messages.latestConversations(viewerId, 100);
            if (latest.isEmpty()) return latest;
            Profile viewer = profiles.findByUserId(viewerId).orElseThrow(() ->
                    new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
            return latest.stream().filter(message -> {
                Long partnerId = viewerId.equals(message.senderId()) ? message.recipientId() : message.senderId();
                return profiles.findByUserId(partnerId)
                        .map(partner -> canReadConversation(viewer, partner, viewerId, partnerId))
                        .orElse(false);
            }).toList();
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
}
