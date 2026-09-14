package glitched.adlips.adapter.in.web.view;

import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.application.view.ContentViewResult;
import glitched.adlips.application.view.ContentViewTarget;
import glitched.adlips.application.view.RecordContentViewUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "콘텐츠 조회수", description = "콘텐츠 조회수 API")
@RequestMapping("/api/v1")
public class ContentViewController {

    private final RecordContentViewUseCase useCase;
    private final ViewIdentityResolver identityResolver;

    public ContentViewController(
            RecordContentViewUseCase useCase,
            ViewIdentityResolver identityResolver
    ) {
        this.useCase = useCase;
        this.identityResolver = identityResolver;
    }

    @PostMapping("/shorts/{shortId}/views")
    public ResponseEntity<ApiResponse<ContentViewResponse>> recordShortView(
            @PathVariable Long shortId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId
    ) {
        return record(ContentViewTarget.SHORT, shortId, authorization, deviceId);
    }

    @PostMapping("/posts/{postId}/views")
    public ResponseEntity<ApiResponse<ContentViewResponse>> recordPostView(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId
    ) {
        return record(ContentViewTarget.POST, postId, authorization, deviceId);
    }

    private ResponseEntity<ApiResponse<ContentViewResponse>> record(
            ContentViewTarget target,
            Long contentId,
            String authorization,
            String deviceId
    ) {
        String viewerId = identityResolver.resolve(authorization, deviceId);
        ContentViewResult result = useCase.execute(target, contentId, viewerId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "조회수 기록 요청이 처리되었습니다.",
                        ContentViewResponse.from(result)
                ));
    }
}
