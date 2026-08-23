package glitched.adlips.adapter.in.web.community;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostCreateRequest;
import glitched.adlips.adapter.in.web.dto.PostCreateResponse;
import glitched.adlips.adapter.in.web.dto.PostDetailResponse;
import glitched.adlips.adapter.in.web.dto.PostListResponse;
import glitched.adlips.adapter.in.web.dto.PostUpdateRequest;
import glitched.adlips.adapter.in.web.dto.PostUpdateResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.DeletePostUseCase;
import glitched.adlips.application.community.GetPostUseCase;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.PostListResult;
import glitched.adlips.application.community.PostResult;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.PostQueryItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/galleries")
public class PostController {

    private final CreatePostUseCase createPostUseCase;
    private final GetPostUseCase getPostUseCase;
    private final GetPostsUseCase getPostsUseCase;
    private final UpdatePostUseCase updatePostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostController(
            CreatePostUseCase createPostUseCase,
            GetPostUseCase getPostUseCase,
            GetPostsUseCase getPostsUseCase,
            UpdatePostUseCase updatePostUseCase,
            DeletePostUseCase deletePostUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.createPostUseCase = createPostUseCase;
        this.getPostUseCase = getPostUseCase;
        this.getPostsUseCase = getPostsUseCase;
        this.updatePostUseCase = updatePostUseCase;
        this.deletePostUseCase = deletePostUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{galleryId}/posts")
    public ResponseEntity<ApiResponse<PostCreateResponse>> create(
            @PathVariable Long galleryId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody PostCreateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        PostResult result = createPostUseCase.execute(galleryId, userId, request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 성공적으로 등록되었습니다.", PostCreateResponse.from(result)));
    }

    @GetMapping("/{galleryId}/posts")
    public ResponseEntity<ApiResponse<PostListResponse>> getPosts(
            @PathVariable Long galleryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PostListResult result = getPostsUseCase.execute(galleryId, page, size);
        return ResponseEntity.ok(ApiResponse.success(
                "게시글 목록 조회가 완료되었습니다.", PostListResponse.from(result)));
    }

    @GetMapping("/{galleryId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @PathVariable Long galleryId,
            @PathVariable Long postId
    ) {
        PostQueryItem item = getPostUseCase.execute(galleryId, postId);
        return ResponseEntity.ok(ApiResponse.success(
                "게시글 상세 조회가 완료되었습니다.", PostDetailResponse.from(item)));
    }

    @PatchMapping("/{galleryId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostUpdateResponse>> update(
            @PathVariable Long galleryId,
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody PostUpdateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        Long updatedPostId = updatePostUseCase.execute(galleryId, postId, userId, request.title(), request.content());
        return ResponseEntity.ok(ApiResponse.success(
                "게시글이 성공적으로 수정되었습니다.", new PostUpdateResponse(updatedPostId)));
    }

    @DeleteMapping("/{galleryId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long galleryId,
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        deletePostUseCase.execute(galleryId, postId, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 성공적으로 삭제되었습니다."));
    }
}
