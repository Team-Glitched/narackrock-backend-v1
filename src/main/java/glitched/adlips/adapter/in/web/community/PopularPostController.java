package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PopularPostListResponse;
import glitched.adlips.application.community.GetPopularPostsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "인기 게시글", description = "갤러리 통합 인기 게시글 API")
@RequestMapping("/api/v1/posts")
public class PopularPostController {

    private final GetPopularPostsUseCase getPopularPostsUseCase;

    public PopularPostController(GetPopularPostsUseCase getPopularPostsUseCase) {
        this.getPopularPostsUseCase = getPopularPostsUseCase;
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PopularPostListResponse>> getPopularPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "인기 게시글 조회가 완료되었습니다.",
                PopularPostListResponse.from(getPopularPostsUseCase.execute(page, size))));
    }
}
