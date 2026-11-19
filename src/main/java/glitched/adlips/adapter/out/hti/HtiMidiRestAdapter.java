package glitched.adlips.adapter.out.hti;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class HtiMidiRestAdapter implements HtiMidiTransformPort {
    private final RestClient client;

    public HtiMidiRestAdapter(RestClient client) {
        this.client = client;
    }

    @Override
    public Result transform(Command command) {
        var multipart = multipart(command.filename(), command.content(), command.contentType(), command.bpm(), command.ppq());
        multipart.part("soundKind", command.soundKind().name());

        try {
            Response response = client.post()
                    .uri("/api/v1/clips/{clipId}/hti-transform", command.clipId())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart.build())
                    .retrieve()
                    .body(Response.class);
            if (response == null || response.midiNotes() == null) {
                throw new HtiServiceException("HTI service returned an empty response", null);
            }
            List<Note> notes = mapNotes(response.midiNotes());
            var route = response.routing();
            if (route == null && command.soundKind() != glitched.adlips.domain.project.AudioTranscriptionMode.HUMMING) {
                throw new HtiServiceException("Python server does not support automatic audio routing", null);
            }
            return new Result(response.durationTick(), notes, route == null ? null
                    : new Routing(route.engine(), route.kind(), route.reason(), route.version()));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 422) throw new InvalidAudioException(exception);
            throw new HtiServiceException("HTI service request failed", exception);
        } catch (RestClientException exception) {
            throw new HtiServiceException("HTI service request failed", exception);
        }
    }

    @Override
    public AudioResult analyze(AudioCommand command) {
        var body = multipart(command.filename(), command.content(), command.contentType(), command.bpm(), command.ppq());
        body.part("soundKind", command.soundKind().name());
        body.part("offsetMs", String.valueOf(command.offsetMs()));
        body.part("durationMs", String.valueOf(command.durationMs()));
        try {
            var response = client.post().uri("/api/v1/audio-analysis")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build()).retrieve().body(AnalysisResponse.class);
            if (response == null || response.midiNotes() == null) {
                throw new HtiServiceException("Audio analysis returned an empty response", null);
            }
            return new AudioResult(response.engineVersion(), mapNotes(response.midiNotes()));
        } catch (RestClientException exception) {
            throw new HtiServiceException("Audio analysis request failed", exception);
        }
    }

    private List<Note> mapNotes(List<ResponseNote> notes) {
        return notes.stream().map(note -> new Note(note.pitch(), note.startTick(), note.durationTick(),
                note.velocity(), note.effectType(), note.effectParams() == null ? Map.of() : note.effectParams())).toList();
    }

    private MultipartBodyBuilder multipart(String filename, byte[] content, String type, int bpm, int ppq) {
        var body = new MultipartBodyBuilder();
        var audio = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? "audio.wav" : filename;
            }
        };
        body.part("audio", audio).contentType(mediaType(type));
        body.part("bpm", String.valueOf(bpm));
        body.part("ppq", String.valueOf(ppq));
        return body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnalysisResponse(String engineVersion, List<ResponseNote> midiNotes) {}

    private MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Response(int durationTick, List<ResponseNote> midiNotes, ResponseRouting routing) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseRouting(String engine, String kind, String reason, String version) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseNote(
            int pitch,
            int startTick,
            int durationTick,
            int velocity,
            String effectType,
            Map<String, Object> effectParams
    ) {
    }
}
