package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.opensource.OpenSourceException;
import glitched.adlips.application.opensource.OpenSourceInteractionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/sources", "/api/v1/open-sources"})
@Tag(name = "샘플 댓글 수정 및 삭제", description = "샘플 댓글 수정 및 삭제 API")
@SecurityRequirement(name = "bearerAuth")
public class OpenSourceCommentController {
    public record UpdateCommentRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000) String content) {}

    public record UpdatedComment(Long commentId) {}

    private final OpenSourceInteractionUseCase useCase;
    private final AuthenticatedUserResolver users;

    public OpenSourceCommentController(OpenSourceInteractionUseCase useCase, AuthenticatedUserResolver users) {
        this.useCase = useCase;
        this.users = users;
    }

    @PutMapping("/comments/{id}")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = UpdateCommentRequest.class))))
    public ApiResponse<UpdatedComment> update(
            @PathVariable Long id,
            @RequestHeader("Authorization") String auth,
            @RequestBody(required = false) Map<String, Object> body) {
        Long user = users.requireUserId(auth);
        if (body == null || !Set.of("content").containsAll(body.keySet()) || !(body.get("content") instanceof String content)) {
            throw new OpenSourceException(OpenSourceException.Code.INVALID_REQUEST, "댓글 입력값이 올바르지 않습니다.");
        }
        return ApiResponse.success("샘플 댓글이 수정되었습니다.", new UpdatedComment(useCase.updateComment(user, id, content)));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        useCase.deleteComment(users.requireUserId(auth), id);
        return ApiResponse.success("샘플 댓글이 삭제되었습니다.");
    }
}
