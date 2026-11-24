package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
public class DirectChatController {
    public record SendMessageRequest(String content) {}
    public record EditMessageRequest(String content) {}
    public record ReadMessagesRequest(Long throughId) {}
    public record UnreadCount(long count) {}
    public record ChatThreadCard(Long userId, String nickname, String profileImageUrl, String primaryInstrument,
                                 Long lastMessageId, String lastMessage, Instant lastMessageAt, boolean outgoing,
                                 long unreadCount) {}
    private final DirectChatUseCase chat;
    private final AuthenticatedUserResolver users;
    private final ChatRealtimePublisher realtime;
    private final ProfileRepositoryPort profiles;
    private final MediaFileRepositoryPort mediaFiles;
    public DirectChatController(DirectChatUseCase chat, AuthenticatedUserResolver users, ChatRealtimePublisher realtime,
                                ProfileRepositoryPort profiles, MediaFileRepositoryPort mediaFiles) {
        this.chat = chat; this.users = users; this.realtime = realtime; this.profiles = profiles; this.mediaFiles = mediaFiles;
    }
    @GetMapping
    public ApiResponse<List<ChatThreadCard>> threads(@RequestHeader("Authorization") String authorization) {
        Long viewerId = users.requireUserId(authorization);
        return ApiResponse.success("최근 대화 목록", chat.latestConversations(viewerId).stream().map(thread -> {
            Long partnerId = thread.partnerId();
            var message = thread.lastMessage();
            var profile = profiles.findByUserId(partnerId).orElse(null);
            String imageUrl = profile == null || profile.getProfileImageFileId() == null ? null
                    : mediaFiles.findById(profile.getProfileImageFileId()).map(MediaFile::getFileUrl).orElse(null);
            String preview = message.deleted() ? "삭제된 메시지입니다." : message.content();
            return new ChatThreadCard(partnerId, profile == null ? "알 수 없는 사용자" : profile.getNickname(), imageUrl,
                    profile == null ? null : profile.getPrimaryInstrument(), message.id(), preview,
                    message.createdAt().atZone(ZoneId.systemDefault()).toInstant(),
                    viewerId.equals(message.senderId()), thread.unreadCount());
        }).toList());
    }
    @GetMapping("/{friendId}/messages")
    public ApiResponse<List<ChatMessagePort.Message>> messages(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("대화 조회", chat.conversation(users.requireUserId(authorization), friendId, beforeId, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCount> unread(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("읽지 않은 메시지 수", new UnreadCount(chat.unreadCount(users.requireUserId(authorization))));
    }

    @PatchMapping("/{friendId}/read")
    public ResponseEntity<Void> markRead(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestBody ReadMessagesRequest request) {
        chat.markRead(users.requireUserId(authorization), friendId, request.throughId());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{friendId}/messages")
    public ApiResponse<ChatMessagePort.Message> send(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestBody SendMessageRequest request) {
        ChatMessagePort.Message message = chat.send(users.requireUserId(authorization), friendId, request.content());
        realtime.publish(message);
        return ApiResponse.success("메시지 전송", message);
    }
    @PatchMapping("/{friendId}/messages/{messageId}")
    public ApiResponse<ChatMessagePort.Message> edit(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @PathVariable Long messageId, @RequestBody EditMessageRequest request) {
        ChatMessagePort.Message updated = chat.edit(users.requireUserId(authorization), friendId, messageId, request.content());
        realtime.publish(updated);
        return ApiResponse.success("메시지 수정", updated);
    }
    @DeleteMapping("/{friendId}/messages/{messageId}")
    public ApiResponse<ChatMessagePort.Message> delete(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @PathVariable Long messageId) {
        ChatMessagePort.Message deleted = chat.delete(users.requireUserId(authorization), friendId, messageId);
        realtime.publish(deleted);
        return ApiResponse.success("메시지 삭제", deleted);
    }
}
