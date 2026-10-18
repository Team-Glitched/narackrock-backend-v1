package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
public class DirectChatController {
    public record SendMessageRequest(String content) {}
    public record ChatThreadCard(Long userId, String nickname, String profileImageUrl, String primaryInstrument,
                                 Long lastMessageId, String lastMessage, LocalDateTime lastMessageAt, boolean outgoing) {}
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
        return ApiResponse.success("최근 대화 목록", chat.latestConversations(viewerId).stream().map(message -> {
            Long partnerId = viewerId.equals(message.senderId()) ? message.recipientId() : message.senderId();
            var profile = profiles.findByUserId(partnerId).orElse(null);
            String imageUrl = profile == null || profile.getProfileImageFileId() == null ? null
                    : mediaFiles.findById(profile.getProfileImageFileId()).map(MediaFile::getFileUrl).orElse(null);
            return new ChatThreadCard(partnerId, profile == null ? "알 수 없는 사용자" : profile.getNickname(), imageUrl,
                    profile == null ? null : profile.getPrimaryInstrument(), message.id(), message.content(),
                    message.createdAt(), viewerId.equals(message.senderId()));
        }).toList());
    }
    @GetMapping("/{friendId}/messages")
    public ApiResponse<List<ChatMessagePort.Message>> messages(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("대화 조회", chat.conversation(users.requireUserId(authorization), friendId, beforeId, size));
    }
    @PostMapping("/{friendId}/messages")
    public ApiResponse<ChatMessagePort.Message> send(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestBody SendMessageRequest request) {
        ChatMessagePort.Message message = chat.send(users.requireUserId(authorization), friendId, request.content());
        realtime.publish(message);
        return ApiResponse.success("메시지 전송", message);
    }
}
