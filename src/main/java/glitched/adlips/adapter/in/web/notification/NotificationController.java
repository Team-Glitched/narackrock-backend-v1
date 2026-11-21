package glitched.adlips.adapter.in.web.notification;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.notification.NotificationInboxUseCase;
import glitched.adlips.domain.notification.NotificationPreferences;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationInboxUseCase inbox;
    private final AuthenticatedUserResolver users;

    public NotificationController(NotificationInboxUseCase inbox, AuthenticatedUserResolver users) {
        this.inbox = inbox;
        this.users = users;
    }

    @GetMapping
    public ApiResponse<NotificationInboxUseCase.Page> list(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.success(
                "알림 조회", inbox.list(users.requireUserId(auth), cursor, category, unreadOnly, size));
    }

    public record Unread(long count) {}

    public record ReadAll(Long throughId) {}

    @GetMapping("/unread-count")
    public ApiResponse<Unread> unread(@RequestHeader("Authorization") String auth) {
        return ApiResponse.success("미확인 알림", new Unread(inbox.unread(users.requireUserId(auth))));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> read(
            @RequestHeader("Authorization") String auth, @PathVariable Long id) {
        inbox.read(users.requireUserId(auth), id);
        return ApiResponse.success("알림을 읽었습니다.");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> all(
            @RequestHeader("Authorization") String auth, @RequestBody ReadAll body) {
        inbox.readAll(users.requireUserId(auth), body.throughId());
        return ApiResponse.success("알림을 모두 읽었습니다.");
    }

    @DeleteMapping("/read")
    public ApiResponse<Void> deleteRead(@RequestHeader("Authorization") String auth) {
        inbox.deleteRead(users.requireUserId(auth));
        return ApiResponse.success("읽은 알림을 모두 삭제했습니다.");
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(
            @RequestHeader("Authorization") String auth, @PathVariable Long id) {
        inbox.delete(users.requireUserId(auth), id);
        return ApiResponse.success("알림을 삭제했습니다.");
    }

    @GetMapping("/preferences")
    public ApiResponse<NotificationPreferences.Values> preferences(
            @RequestHeader("Authorization") String auth) {
        return ApiResponse.success("알림 설정", inbox.preferences(users.requireUserId(auth)));
    }

    @PutMapping("/preferences")
    public ApiResponse<NotificationPreferences.Values> preferences(
            @RequestHeader("Authorization") String auth,
            @RequestBody NotificationPreferences.Values values) {
        Long user = users.requireUserId(auth);
        inbox.preferences(user, values);
        return ApiResponse.success("알림 설정 저장", values);
    }
}
