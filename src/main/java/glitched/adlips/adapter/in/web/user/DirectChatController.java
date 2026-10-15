package glitched.adlips.adapter.in.web.user;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
public class DirectChatController {
    public record SendMessageRequest(String content) {}
    private final DirectChatUseCase chat;
    private final AuthenticatedUserResolver users;
    public DirectChatController(DirectChatUseCase chat, AuthenticatedUserResolver users) { this.chat = chat; this.users = users; }
    @GetMapping("/{friendId}/messages")
    public ApiResponse<List<ChatMessagePort.Message>> messages(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("대화 조회", chat.conversation(users.requireUserId(authorization), friendId, beforeId, size));
    }
    @PostMapping("/{friendId}/messages")
    public ApiResponse<ChatMessagePort.Message> send(@RequestHeader("Authorization") String authorization,
            @PathVariable Long friendId, @RequestBody SendMessageRequest request) {
        return ApiResponse.success("메시지 전송", chat.send(users.requireUserId(authorization), friendId, request.content()));
    }
}
