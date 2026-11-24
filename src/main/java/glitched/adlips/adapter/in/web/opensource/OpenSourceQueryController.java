package glitched.adlips.adapter.in.web.opensource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import glitched.adlips.adapter.in.web.ApiResponse;
import glitched.adlips.adapter.in.web.user.AuthenticatedUserResolver;
import glitched.adlips.application.opensource.OpenSourceQueryPort;
import glitched.adlips.application.opensource.OpenSourceQueryUseCase;
import glitched.adlips.application.opensource.OpenSourceQueryUseCase.Page;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "샘플 조회", description = "공개 목록·검색·상세, 내 보관함 및 등록 목록. 로그인 필요")
@SecurityRequirement(name = "bearerAuth")
public class OpenSourceQueryController {
    private static final ObjectMapper MIDI_JSON = new ObjectMapper();
    private static final TypeReference<List<MidiNoteItem>> MIDI_LIST_TYPE = new TypeReference<>() {};

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Writer(Long userId, String nickname, String profileImageUrl) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MidiNoteItem(int pitch, int startTick, int durationTick, int velocity) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Summary(Long openSourceId, String title, String description, String thumbnailUrl, Writer writer,
            Long usageCount, boolean canPreview, boolean isSaved, boolean isLiked, boolean isDisliked,
            long likeCount, long dislikeCount, long commentCount, Instant createdAt, String instrument,
            boolean hasMidiNotes) {
        static Summary from(OpenSourceQueryPort.Item item) {
            var writer = item.writer();
            return new Summary(item.openSourceId(), item.title(), item.description(), item.thumbnailUrl(),
                    new Writer(writer.userId(), writer.nickname(), writer.profileImageUrl()),
                    item.usageCount(), item.canPreview(), item.isSaved(), item.isLiked(), item.isDisliked(),
                    item.likeCount(), item.dislikeCount(), item.commentCount(), item.createdAt(),
                    item.instrument(), item.hasMidiNotes());
        }
    }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Detail(@JsonUnwrapped Summary source, Long durationMs,
                         Integer midiPpq, Integer midiBpm, List<MidiNoteItem> midiNotes) {
        static Detail from(OpenSourceQueryPort.Item item) {
            List<MidiNoteItem> notes = parseMidiNotes(item.midiNotesJson());
            return new Detail(Summary.from(item), null,
                    notes == null ? null : item.midiPpq(),
                    notes == null ? null : item.midiBpm(),
                    notes);
        }

        private static List<MidiNoteItem> parseMidiNotes(String json) {
            if (json == null || json.isBlank()) return null;
            try {
                return MIDI_JSON.readValue(json, MIDI_LIST_TYPE);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
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

    @GetMapping({"/api/v1/sources", "/api/v1/open-sources"})
    public ApiResponse<Page<Summary>> list(@RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.list(users.requireUserId(authorization), keyword, page, size);
        return ApiResponse.success("샘플 목록 조회가 완료되었습니다.",
                new Page<>(result.page(), result.size(), result.hasNext(), result.items().stream().map(Summary::from).toList()));
    }

    @GetMapping({"/api/v1/sources/popular", "/api/v1/open-sources/popular"})
    public ApiResponse<Page<Summary>> popular(@RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "10") int size) {
        var result = useCase.popular(users.requireUserId(authorization), size);
        return ApiResponse.success("인기 샘플 목록을 조회했습니다.",
                new Page<>(result.page(), result.size(), result.hasNext(), result.items().stream().map(Summary::from).toList()));
    }

    @GetMapping({"/api/v1/sources/{id}", "/api/v1/open-sources/{id}"})
    public ApiResponse<Detail> detail(@PathVariable Long id, @RequestHeader("Authorization") String authorization) {
        var item = useCase.detail(users.requireUserId(authorization), id);
        return ApiResponse.success("샘플 상세 조회가 완료되었습니다.", Detail.from(item));
    }

    @GetMapping({"/api/v1/users/me/sources", "/api/v1/users/me/open-sources"})
    public ApiResponse<Page<Saved>> saved(@RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.saved(users.requireUserId(authorization), page, size);
        return ApiResponse.success("샘플 보관함 조회가 완료되었습니다.",
                new Page<>(result.page(), result.size(), result.hasNext(), result.items().stream().map(Saved::from).toList()));
    }

    @GetMapping({"/api/v1/users/me/source-publications", "/api/v1/users/me/open-source-publications"})
    public ApiResponse<Page<OpenSourcePublicationController.PublicationResponse>> publications(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.publications(users.requireUserId(authorization), page, size);
        return ApiResponse.success("내 샘플 등록 목록을 조회했습니다.", new Page<>(result.page(), result.size(), result.hasNext(),
                result.items().stream().map(OpenSourcePublicationController.PublicationResponse::from).toList()));
    }
}
