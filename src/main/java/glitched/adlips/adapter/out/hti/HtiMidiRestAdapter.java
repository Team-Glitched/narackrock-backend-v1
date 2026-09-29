package glitched.adlips.adapter.out.hti;

import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class HtiMidiRestAdapter implements HtiMidiTransformPort {
    private final RestClient client;

    public HtiMidiRestAdapter(RestClient client) {
        this.client = client;
    }

    @Override
    public Result transform(Command command) {
        var multipart = new MultipartBodyBuilder();
        var audio = new ByteArrayResource(command.content()) {
            @Override
            public String getFilename() {
                return command.filename();
            }
        };
        multipart.part("audio", audio)
                .contentType(mediaType(command.contentType()));
        multipart.part("bpm", command.bpm());
        multipart.part("ppq", command.ppq());

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
            List<Note> notes = response.midiNotes().stream()
                    .map(note -> new Note(
                            note.pitch(), note.startTick(), note.durationTick(), note.velocity(),
                            note.effectType(), note.effectParams() == null ? Map.of() : note.effectParams()))
                    .toList();
            return new Result(response.durationTick(), notes);
        } catch (RestClientException exception) {
            throw new HtiServiceException("HTI service request failed", exception);
        }
    }

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

    private record Response(int durationTick, List<ResponseNote> midiNotes) {
    }

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
