package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.opensource.OpenSourceException;
import glitched.adlips.application.opensource.OpenSourcePublicationUseCase;
import glitched.adlips.domain.opensource.OpenSourcePublication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "샘플 등록 및 공개", description = "작성자 전용 등록·메타데이터 수정·공개 설정·삭제·관리 상세")
@SecurityRequirement(name = "bearerAuth")
public class OpenSourcePublicationController {
    @Schema(description = "본인 소유 READY 오디오로 비공개 등록. 알 수 없는 필드는 거부합니다.")
    public record CreateRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 100, example = "기타 리프") String title,
            @Schema(maxLength = 2000) String description,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") Long mediaFileId,
            @Schema(minimum = "1") Long thumbnailMediaFileId) {}
    @Schema(description = "한 개 이상의 필드가 필요합니다. 생략은 유지, 설명/썸네일의 null은 제거합니다. 음원/소유자는 변경할 수 없습니다.")
    public record UpdateRequest(
            @Schema(minLength = 1, maxLength = 100) String title,
            @Schema(maxLength = 2000, nullable = true) String description,
            @Schema(minimum = "1", nullable = true) Long thumbnailMediaFileId) {}
    public record VisibilityRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"PRIVATE", "PUBLIC"}) String visibility) {}

    public record PublicationResponse(Long openSourceId, Long ownerId, String title, String description,
            Long mediaFileId, Long thumbnailMediaFileId, String visibility, Instant createdAt, Instant updatedAt) {
        static PublicationResponse from(OpenSourcePublication source) {
            return new PublicationResponse(source.id(), source.ownerId(), source.title(), source.description(),
                    source.mediaFileId(), source.thumbnailMediaFileId(), source.isPublic() ? "PUBLIC" : "PRIVATE",
                    source.createdAt(), source.updatedAt());
        }
    }
    public record VisibilityResponse(Long openSourceId, String visibility) {}
    public record DeleteResponse(Long openSourceId, boolean isDeleted) {}
    private final OpenSourcePublicationUseCase useCase;
    private final AuthenticatedUserResolver users;

    public OpenSourcePublicationController(OpenSourcePublicationUseCase useCase, AuthenticatedUserResolver users) {
        this.useCase = useCase;
        this.users = users;
    }

    @PostMapping({"/api/v1/sources", "/api/v1/open-sources"})
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = CreateRequest.class))))
    public ResponseEntity<ApiResponse<PublicationResponse>> create(
            @RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        Long userId = users.requireUserId(authorization);
        fields(body, Set.of("title", "description", "mediaFileId", "thumbnailMediaFileId"));
        var source = useCase.create(userId, text(body, "title"), text(body, "description"),
                id(body, "mediaFileId"), id(body, "thumbnailMediaFileId"));
        return ResponseEntity.status(201).body(ApiResponse.success("샘플을 비공개로 등록했습니다.", PublicationResponse.from(source)));
    }

    @PatchMapping({"/api/v1/sources/{id}", "/api/v1/open-sources/{id}"})
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = UpdateRequest.class))))
    public ApiResponse<PublicationResponse> update(@PathVariable Long id,
            @RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        Long userId = users.requireUserId(authorization);
        fields(body, Set.of("title", "description", "thumbnailMediaFileId"));
        var patch = new OpenSourcePublicationUseCase.Patch(body.containsKey("title"), text(body, "title"),
                body.containsKey("description"), text(body, "description"),
                body.containsKey("thumbnailMediaFileId"), id(body, "thumbnailMediaFileId"));
        return ApiResponse.success("샘플을 수정했습니다.", PublicationResponse.from(useCase.update(userId, id, patch)));
    }

    @PutMapping({"/api/v1/sources/{id}/visibility", "/api/v1/open-sources/{id}/visibility"})
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = VisibilityRequest.class))))
    public ApiResponse<VisibilityResponse> visibility(@PathVariable Long id,
            @RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        Long userId = users.requireUserId(authorization);
        fields(body, Set.of("visibility"));
        String visibility = text(body, "visibility");
        if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility)) throw invalid();
        var source = useCase.visibility(userId, id, "PUBLIC".equals(visibility));
        return ApiResponse.success("샘플 공개 상태를 변경했습니다.", new VisibilityResponse(source.id(), source.isPublic() ? "PUBLIC" : "PRIVATE"));
    }

    @DeleteMapping({"/api/v1/sources/{id}", "/api/v1/open-sources/{id}"})
    public ApiResponse<DeleteResponse> delete(@PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        var source = useCase.delete(users.requireUserId(authorization), id);
        return ApiResponse.success("샘플을 삭제했습니다.", new DeleteResponse(source.id(), true));
    }

    @GetMapping({"/api/v1/users/me/source-publications/{id}", "/api/v1/users/me/open-source-publications/{id}"})
    public ApiResponse<PublicationResponse> detail(@PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("샘플 관리 정보를 조회했습니다.", PublicationResponse.from(useCase.get(users.requireUserId(authorization), id)));
    }

    private void fields(Map<String, Object> body, Set<String> allowed) {
        if (body == null || body.isEmpty() || !allowed.containsAll(body.keySet())) throw invalid();
    }
    private String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) return null;
        if (!(value instanceof String text)) throw invalid();
        return text;
    }
    private Long id(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) return null;
        if (value instanceof Integer number) return number.longValue();
        if (value instanceof Long number) return number;
        throw invalid();
    }
    private OpenSourceException invalid() {
        return new OpenSourceException(OpenSourceException.Code.INVALID_REQUEST, "요청 필드 또는 값이 올바르지 않습니다.");
    }
}
