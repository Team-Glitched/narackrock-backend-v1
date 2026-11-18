package glitched.adlips.adapter.out.hti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import glitched.adlips.application.project.port.out.HtiMidiTransformPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HtiMidiRestAdapterTest {
    @Test
    void sendsMultipartAudioAndMapsMidiNotes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://hti.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://hti.test/api/v1/clips/10/hti-transform"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("name=\"audio\"")))
                .andExpect(content().string(containsString("name=\"bpm\"")))
                .andExpect(content().string(containsString("120")))
                .andExpect(content().string(containsString("name=\"ppq\"")))
                .andExpect(content().string(containsString("480")))
                .andExpect(content().string(containsString("name=\"soundKind\"")))
                .andExpect(content().string(containsString("AUTO")))
                .andRespond(withSuccess("""
                        {
                          "durationTick": 960,
                          "routing": {"engine":"BASIC_PITCH","kind":"POLYPHONIC","reason":"여러 음","version":"signal-rules-v1","metrics":{}},
                          "midiNotes": [{
                            "pitch": 64,
                            "startTick": 0,
                            "durationTick": 480,
                            "velocity": 100,
                            "effectType": null,
                            "effectParams": {}
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        var adapter = new HtiMidiRestAdapter(builder.build());

        var result = adapter.transform(new HtiMidiTransformPort.Command(
                10L, "recording.wav", "audio/wav", new byte[]{1, 2, 3}, 120, 480));

        assertThat(result.durationTick()).isEqualTo(960);
        assertThat(result.routing().engine()).isEqualTo("BASIC_PITCH");
        assertThat(result.routing().kind()).isEqualTo("POLYPHONIC");
        assertThat(result.midiNotes()).hasSize(1);
        assertThat(result.midiNotes().getFirst().pitch()).isEqualTo(64);
        server.verify();
    }

    @Test
    void invalidAudioRemainsAnInputError() {
        var builder = RestClient.builder().baseUrl("http://hti.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://hti.test/api/v1/clips/10/hti-transform"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        var adapter = new HtiMidiRestAdapter(builder.build());
        assertThatThrownBy(() -> adapter.transform(new HtiMidiTransformPort.Command(
                10L, "silence.wav", "audio/wav", new byte[]{1}, 120, 480)))
                .isInstanceOf(HtiMidiTransformPort.InvalidAudioException.class);
        server.verify();
    }

    @Test
    void refusesAutomaticConversionAgainstAnOlderMonophonicServer() {
        var builder = RestClient.builder().baseUrl("http://hti.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://hti.test/api/v1/clips/10/hti-transform"))
                .andRespond(withSuccess("{\"durationTick\":960,\"midiNotes\":[]}", MediaType.APPLICATION_JSON));
        var adapter = new HtiMidiRestAdapter(builder.build());
        assertThatThrownBy(() -> adapter.transform(new HtiMidiTransformPort.Command(
                10L, "piano.wav", "audio/wav", new byte[]{1}, 120, 480)))
                .isInstanceOf(HtiMidiTransformPort.HtiServiceException.class);
        server.verify();
    }
}
