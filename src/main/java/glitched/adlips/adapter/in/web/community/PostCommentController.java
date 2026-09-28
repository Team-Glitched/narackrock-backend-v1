package glitched.adlips.adapter.in.web.community;

import io.swagger.v3.oas.annotations.tags.Tag;

import glitched.adlips.adapter.in.web.dto.ApiResponse;
import glitched.adlips.adapter.in.web.dto.PostCommentCreateRequest;
import glitched.adlips.adapter.in.web.dto.PostCommentCreateResponse;
import glitched.adlips.adapter.in.web.dto.PostCommentUpdateRequest;
import glitched.adlips.adapter.in.web.dto.PostCommentUpdateResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.community.CreatePostCommentUseCase;
import glitched.adlips.application.community.PostCommentResult;
import glitched.adlips.application.community.DeletePostCommentUseCase;
import glitched.adlips.application.community.GetPostCommentsUseCase;
import glitched.adlips.application.community.UpdatePostCommentUseCase;
import glitched.adlips.adapter.in.web.dto.PostCommentListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시글 댓글", description = "게시글 댓글 API")
@RestController
@RequestMapping("/api/v1/posts")
public class PostCommentController {

    private final CreatePostCommentUseCase createPostCommentUseCase;
    private final GetPostCommentsUseCase getPostCommentsUseCase;
    private final UpdatePostCommentUseCase updatePostCommentUseCase;
    private final DeletePostCommentUseCase deletePostCommentUseCase;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PostCommentController(
            CreatePostCommentUseCase createPostCommentUseCase,
            GetPostCommentsUseCase getPostCommentsUseCase,
            UpdatePostCommentUseCase updatePostCommentUseCase,
            DeletePostCommentUseCase deletePostCommentUseCase,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.createPostCommentUseCase = createPostCommentUseCase;
        this.getPostCommentsUseCase = getPostCommentsUseCase;
        this.updatePostCommentUseCase = updatePostCommentUseCase;
        this.deletePostCommentUseCase = deletePostCommentUseCase;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<PostCommentCreateResponse>> create(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody PostCommentCreateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        PostCommentResult result = createPostCommentUseCase.execute(
                postId, userId, request.content(), request.parentCommentId());
        String message = result.isReply()
                ? "대댓글이 성공적으로 등록되었습니다." : "댓글이 성공적으로 등록되었습니다.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, PostCommentCreateResponse.from(result)));
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<PostCommentListResponse> list(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long parentCommentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        return ApiResponse.success("댓글 목록을 조회했습니다.", PostCommentListResponse.from(
                getPostCommentsUseCase.execute(postId, userId, parentCommentId, page, size)));
    }

    // 댓글 id가 전역 고유해서 사양상 postId를 경로에 두지 않는다.
    @PutMapping("/comments/{commentId}")
    public ApiResponse<PostCommentUpdateResponse> update(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody PostCommentUpdateRequest request
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        Long updatedCommentId = updatePostCommentUseCase.execute(commentId, userId, request.content());
        return ApiResponse.success(
                "댓글이 성공적으로 수정되었습니다.", new PostCommentUpdateResponse(updatedCommentId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long userId = authenticatedUserResolver.requireUserId(authorization);
        deletePostCommentUseCase.execute(commentId, userId);
        return ApiResponse.success("댓글이 성공적으로 삭제되었습니다.", null);
    }
}
