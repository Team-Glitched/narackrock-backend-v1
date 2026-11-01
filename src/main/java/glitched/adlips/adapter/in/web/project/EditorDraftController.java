package glitched.adlips.adapter.in.web.project;

import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.project.dto.request.EditorDraftBatchRequest;
import glitched.adlips.application.project.dto.request.EditorDraftSaveRequest;
import glitched.adlips.application.project.dto.response.ProjectContributionCreateResponse;
import glitched.adlips.application.project.usecase.EditorDraftUseCase;
import glitched.adlips.application.project.usecase.EditorDraftWorkflowUseCase;
import glitched.adlips.domain.project.EditorDraft;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@Tag(name = "개인 작곡 초안", description = "공동 작업 원본과 분리된 개인 MIDI 작업 초안")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class EditorDraftController {
    private final EditorDraftUseCase drafts;
    private final EditorDraftWorkflowUseCase workflow;
    private final AuthenticatedUserResolver resolver;
    public record RevisionRequest(long expectedRevision) {}

    public EditorDraftController(EditorDraftUseCase drafts, EditorDraftWorkflowUseCase workflow,
                                  AuthenticatedUserResolver resolver) {
        this.drafts = drafts; this.workflow = workflow; this.resolver = resolver;
    }

    @GetMapping("/editor-drafts")
    public ApiResponse<List<EditorDraft>> list(@PathVariable long projectId,
                                              @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("개인 초안을 조회했습니다.", drafts.list(projectId, resolver.requireUserId(authorization)));
    }

    @GetMapping("/editor-drafts/{draftId}")
    public ApiResponse<EditorDraft> get(@PathVariable long projectId, @PathVariable UUID draftId,
                                       @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("개인 초안을 조회했습니다.", drafts.get(projectId, draftId, resolver.requireUserId(authorization)));
    }

    @PutMapping("/tracks/{trackId}/editor-drafts/{draftId}")
    public ApiResponse<EditorDraft> save(@PathVariable long projectId, @PathVariable long trackId,
                                        @PathVariable UUID draftId, @RequestHeader("Authorization") String authorization,
                                        @RequestBody EditorDraftSaveRequest request) {
        return ApiResponse.success("개인 초안을 저장했습니다.",
                drafts.save(projectId, trackId, draftId, resolver.requireUserId(authorization), request));
    }

    @PostMapping("/editor-drafts/{draftId}/commit")
    public ApiResponse<EditorDraft> commit(@PathVariable long projectId, @PathVariable UUID draftId,
                                          @RequestHeader("Authorization") String authorization,
                                          @RequestBody RevisionRequest request) {
        return ApiResponse.success("초안을 레이어에 반영했습니다.",
                drafts.commit(projectId, draftId, resolver.requireUserId(authorization), request.expectedRevision()));
    }

    @DeleteMapping("/editor-drafts/{draftId}")
    public ApiResponse<EditorDraft> discard(@PathVariable long projectId, @PathVariable UUID draftId,
                                           @RequestHeader("Authorization") String authorization,
                                           @RequestParam long expectedRevision) {
        return ApiResponse.success("개인 초안을 버렸습니다.",
                drafts.discard(projectId, draftId, resolver.requireUserId(authorization), expectedRevision));
    }

    @PostMapping("/editor-drafts/commit")
    public ApiResponse<List<EditorDraft>> commitAll(@PathVariable long projectId,
                                                   @RequestHeader("Authorization") String authorization,
                                                   @RequestBody EditorDraftBatchRequest request) {
        return ApiResponse.success("개인 초안을 레이어에 반영했습니다.",
                workflow.commitAll(projectId, resolver.requireUserId(authorization), request));
    }

    @PostMapping("/editor-drafts/submit")
    public ApiResponse<ProjectContributionCreateResponse> submit(@PathVariable long projectId,
                                                                @RequestHeader("Authorization") String authorization,
                                                                @RequestBody EditorDraftBatchRequest request) {
        return ApiResponse.success("작업을 제출했습니다.",
                workflow.submit(projectId, resolver.requireUserId(authorization), request));
    }
}
