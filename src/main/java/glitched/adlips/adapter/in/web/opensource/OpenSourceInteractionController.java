package glitched.adlips.adapter.in.web.opensource;

import com.fasterxml.jackson.annotation.JsonInclude;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.adapter.in.web.view.ContentViewResponse;
import glitched.adlips.application.opensource.*;
import glitched.adlips.domain.reaction.ReactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static glitched.adlips.application.opensource.OpenSourceInteractionPort.*;

@RestController
@RequestMapping("/api/v1/open-sources")
@Tag(name="오픈소스 반응 및 댓글")
@SecurityRequirement(name="bearerAuth")
public class OpenSourceInteractionController {
    public record SourceReaction(Long openSourceId,boolean isLiked,boolean isDisliked,long likeCount,long dislikeCount) {}
    public record CommentReaction(Long commentId,boolean isLiked,boolean isDisliked,long likeCount,long dislikeCount) {}
    public record CreateRequest(@Schema(requiredMode=Schema.RequiredMode.REQUIRED,minLength=1,maxLength=1000) String content,Long parentCommentId) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CommentResponse(Long commentId,Long parentCommentId,OpenSourceQueryController.Writer writer,String content,
            long replyCount,boolean isLiked,boolean isDisliked,long likeCount,long dislikeCount,Instant createdAt) {
        static CommentResponse from(Comment c) {
            return new CommentResponse(c.commentId(),c.parentCommentId(),new OpenSourceQueryController.Writer(c.writer().userId(),c.writer().nickname(),c.writer().profileImageUrl()),
                    c.content(),c.replyCount(),c.isLiked(),c.isDisliked(),c.likeCount(),c.dislikeCount(),c.createdAt());
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CommentPage(Long openSourceId,Long parentCommentId,int page,int size,long totalCount,boolean hasNext,List<CommentResponse> comments) {}
    public record Created(Long openSourceId,long commentCount,CommentResponse comment) {}
    private final OpenSourceInteractionUseCase useCase;
    private final AuthenticatedUserResolver users;
    public OpenSourceInteractionController(OpenSourceInteractionUseCase useCase,AuthenticatedUserResolver users) { this.useCase=useCase;this.users=users; }
    @PostMapping("/{id}/views")
    public ResponseEntity<ApiResponse<ContentViewResponse>> view(
            @PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        boolean counted = useCase.recordView(users.requireUserId(authorization), id);
        return ResponseEntity.accepted().body(ApiResponse.success("오픈소스 조회수 기록 요청이 처리되었습니다.",
                new ContentViewResponse(id, counted)));
    }

    @PostMapping("/{id}/likes")
    public ApiResponse<SourceReaction> like(@PathVariable Long id,@RequestHeader("Authorization") String auth) { return sourceReaction(id,auth,ReactionType.LIKE); }
    @PostMapping("/{id}/dislikes")
    public ApiResponse<SourceReaction> dislike(@PathVariable Long id,@RequestHeader("Authorization") String auth) { return sourceReaction(id,auth,ReactionType.DISLIKE); }
    private ApiResponse<SourceReaction> sourceReaction(Long id,String auth,ReactionType type) {
        var result=useCase.react(users.requireUserId(auth),id,Target.SOURCE,type);
        return ApiResponse.success("오픈소스 반응 상태가 변경되었습니다.",new SourceReaction(id,result.isLiked(),result.isDisliked(),result.likeCount(),result.dislikeCount()));
    }
    @PostMapping("/comments/{id}/likes")
    public ApiResponse<CommentReaction> likeComment(@PathVariable Long id,@RequestHeader("Authorization") String auth) { return commentReaction(id,auth,ReactionType.LIKE); }
    @PostMapping("/comments/{id}/dislikes")
    public ApiResponse<CommentReaction> dislikeComment(@PathVariable Long id,@RequestHeader("Authorization") String auth) { return commentReaction(id,auth,ReactionType.DISLIKE); }
    private ApiResponse<CommentReaction> commentReaction(Long id,String auth,ReactionType type) {
        var result=useCase.react(users.requireUserId(auth),id,Target.COMMENT,type);
        return ApiResponse.success("오픈소스 댓글 반응 상태가 변경되었습니다.",new CommentReaction(id,result.isLiked(),result.isDisliked(),result.likeCount(),result.dislikeCount()));
    }
    @PostMapping("/{id}/comments")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(schema=@Schema(implementation=CreateRequest.class))))
    public ResponseEntity<ApiResponse<Created>> create(@PathVariable Long id,@RequestHeader("Authorization") String auth,@RequestBody Map<String,Object> body) {
        Long user=users.requireUserId(auth);
        if(body==null || !Set.of("content","parentCommentId").containsAll(body.keySet()) || !(body.get("content") instanceof String content)) throw invalid();
        Object parent=body.get("parentCommentId");
        if(parent!=null && !(parent instanceof Integer) && !(parent instanceof Long)) throw invalid();
        var result=useCase.create(user,id,content,parent==null?null:((Number)parent).longValue());
        return ResponseEntity.status(201).body(ApiResponse.success("오픈소스 댓글이 등록되었습니다.",new Created(result.openSourceId(),result.commentCount(),CommentResponse.from(result.comment()))));
    }
    @PutMapping("/comments/{id}/hidden")
    public ResponseEntity<Void> hideComment(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        useCase.hideComment(users.requireUserId(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<CommentPage> comments(@PathVariable Long id,@RequestHeader("Authorization") String auth,
            @RequestParam(required=false) Long parentCommentId,@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Integer size) {
        var result=useCase.comments(users.requireUserId(auth),id,parentCommentId,page,size==null?(parentCommentId==null?20:10):size);
        return ApiResponse.success("오픈소스 댓글 목록을 조회했습니다.",new CommentPage(result.openSourceId(),result.parentCommentId(),result.page(),result.size(),result.totalCount(),result.hasNext(),result.comments().stream().map(CommentResponse::from).toList()));
    }
    private OpenSourceException invalid() { return new OpenSourceException(OpenSourceException.Code.INVALID_REQUEST,"댓글 입력값이 올바르지 않습니다."); }
}
