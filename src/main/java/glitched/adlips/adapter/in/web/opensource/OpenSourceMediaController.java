package glitched.adlips.adapter.in.web.opensource;

import com.fasterxml.jackson.annotation.JsonInclude;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.opensource.OpenSourceMediaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "오픈소스 재생·공유")
public class OpenSourceMediaController {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Playback(Long openSourceId, String audioUrl, String mimeType, Long durationMs, Instant expiresAt) {}
    private final OpenSourceMediaUseCase useCase;
    private final AuthenticatedUserResolver users;
    public OpenSourceMediaController(OpenSourceMediaUseCase useCase, AuthenticatedUserResolver users) {
        this.useCase = useCase; this.users = users;
    }
    @GetMapping("/api/v1/open-sources/{id}/playback")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Playback>> playback(@PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        var result = useCase.playback(users.requireUserId(authorization), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success("오픈소스 재생 정보를 조회했습니다.",
                new Playback(id,result.audioUrl(),result.mimeType(),result.durationMs(),result.expiresAt())));
    }
    @GetMapping("/api/v1/open-sources/{id}/share")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<OpenSourceMediaUseCase.Share> share(@PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        return ApiResponse.success("오픈소스 공유 링크를 조회했습니다.",useCase.share(users.requireUserId(authorization),id));
    }
    @GetMapping("/api/v1/open-sources/{id}/audio")
    @Operation(summary = "서명된 재생 URL로 음원 요청", description = "로그인 후 발급한 유효한 서명이 필요합니다. Range 요청 지원")
    public ResponseEntity<Resource> audio(@PathVariable Long id, @RequestParam Long mediaId,
            @RequestParam long expires, @RequestParam String signature) {
        var content = useCase.content(id,mediaId,expires,signature);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options","nosniff")
                .contentType(MediaType.parseMediaType(content.mimeType()))
                .body(new AbstractResource() {
                    @Override public String getDescription() { return "Open source audio " + id; }
                    @Override public long contentLength() { return content.length(); }
                    @Override public java.io.InputStream getInputStream() { return content.stream().get(); }
                });
    }
}
