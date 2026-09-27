package glitched.adlips.adapter.in.web.bookmark;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.BookmarkedPostListResponse;
import glitched.adlips.adapter.in.web.dto.BookmarkedShortListResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.bookmark.GetBookmarkedPostsUseCase;
import glitched.adlips.application.bookmark.GetBookmarkedShortsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "내 북마크", description = "내가 북마크한 게시글·숏폼 목록 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users/me/bookmarks")
public class BookmarkController {

    private final GetBookmarkedPostsUseCase getBookmarkedPostsUseCase;
    private final GetBookmarkedShortsUseCase getBookmarkedShortsUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public BookmarkController(
            GetBookmarkedPostsUseCase getBookmarkedPostsUseCase,
            GetBookmarkedShortsUseCase getBookmarkedShortsUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.getBookmarkedPostsUseCase = getBookmarkedPostsUseCase;
        this.getBookmarkedShortsUseCase = getBookmarkedShortsUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<BookmarkedPostListResponse>> getBookmarkedPosts(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "북마크한 게시글 목록 조회가 완료되었습니다.",
                BookmarkedPostListResponse.from(getBookmarkedPostsUseCase.execute(userId, page, size))));
    }

    @GetMapping("/shorts")
    public ResponseEntity<ApiResponse<BookmarkedShortListResponse>> getBookmarkedShorts(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                "북마크한 숏폼 목록 조회가 완료되었습니다.",
                BookmarkedShortListResponse.from(getBookmarkedShortsUseCase.execute(userId, cursor, size))));
    }
}
