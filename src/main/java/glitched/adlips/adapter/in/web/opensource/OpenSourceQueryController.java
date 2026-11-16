package glitched.adlips.adapter.in.web.opensource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.opensource.OpenSourceQueryPort;
import glitched.adlips.application.opensource.OpenSourceQueryUseCase;
import glitched.adlips.application.opensource.OpenSourceQueryUseCase.Page;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "오픈소스 조회", description = "공개 목록·검색·상세, 내 보관함 및 등록 목록. 로그인 필요")
@SecurityRequirement(name = "bearerAuth")
public class OpenSourceQueryController {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Writer(Long userId, String nickname, String profileImageUrl) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Summary(Long openSourceId, String title, String description, String thumbnailUrl, Writer writer,
            Long usageCount, boolean canPreview, boolean isSaved, boolean isLiked, boolean isDisliked,
            long likeCount, long dislikeCount, long commentCount, Instant createdAt) {
        static Summary from(OpenSourceQueryPort.Item item) {
            var writer = item.writer();
            // Usage is unavailable until project insertion can be tracked.
            return new Summary(item.openSourceId(), item.title(), item.description(), item.thumbnailUrl(),
                    new Writer(writer.userId(), writer.nickname(), writer.profileImageUrl()),
                    null, item.canPreview(), item.isSaved(), item.isLiked(), item.isDisliked(), item.likeCount(), item.dislikeCount(), item.commentCount(), item.createdAt());
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Detail(@JsonUnwrapped Summary source, Long durationMs) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Saved(Long openSourceId, Instant savedAt, String availability, Summary source) {
        static Saved from(OpenSourceQueryPort.SavedItem item) {
            return new Saved(item.openSourceId(), item.savedAt(), item.source() == null ? "UNAVAILABLE" : "AVAILABLE",
                    item.source() == null ? null : Summary.from(item.source()));
        }
    }
    private final OpenSourceQueryUseCase useCase;
    private final AuthenticatedUserResolver users;

    public OpenSourceQueryController(OpenSourceQueryUseCase useCase, AuthenticatedUserResolver users) {
        this.useCase = useCase;
        this.users = users;
    }

    @GetMapping("/api/v1/open-sources")
    public ApiResponse<Page<Summary>> list(@RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.list(users.requireUserId(authorization), keyword, page, size);
        return ApiResponse.success("오픈소스 목록 조회가 완료되었습니다.",
                new Page<>(result.page(), result.size(), result.hasNext(), result.items().stream().map(Summary::from).toList()));
    }

    @GetMapping("/api/v1/open-sources/popular")
    public ApiResponse<Page<Summary>> popular(@RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "10") int size) {
        var result = useCase.popular(users.requireUserId(authorization), size);
        return ApiResponse.success("인기 오픈소스 목록을 조회했습니다.",
                new Page<>(result.page(), result.size(), result.hasNext(), result.items().stream().map(Summary::from).toList()));
    }

    @GetMapping("/api/v1/open-sources/{id}")
    public ApiResponse<Detail> detail(@PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        var item = useCase.detail(users.requireUserId(authorization), id);
        return ApiResponse.success("오픈소스 상세 조회가 완료되었습니다.", new Detail(Summary.from(item), null));
    }

    @GetMapping("/api/v1/users/me/open-sources")
    public ApiResponse<Page<Saved>> saved(@RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.saved(users.requireUserId(authorization), page, size);
        return ApiResponse.success("오픈소스 보관함 조회가 완료되었습니다.",
                new Page<>(result.page(), result.size(), result.hasNext(), result.items().stream().map(Saved::from).toList()));
    }

    @GetMapping("/api/v1/users/me/open-source-publications")
    public ApiResponse<Page<OpenSourcePublicationController.PublicationResponse>> publications(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.publications(users.requireUserId(authorization), page, size);
        return ApiResponse.success("내 오픈소스 등록 목록을 조회했습니다.", new Page<>(result.page(), result.size(), result.hasNext(),
                result.items().stream().map(OpenSourcePublicationController.PublicationResponse::from).toList()));
    }
}
