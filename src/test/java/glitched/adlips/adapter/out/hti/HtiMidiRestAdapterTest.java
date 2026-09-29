package glitched.adlips.adapter.out.hti;

import static org.assertj.core.api.Assertions.assertThat;
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
                .andRespond(withSuccess("""
                        {
                          "durationTick": 960,
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
        assertThat(result.midiNotes()).hasSize(1);
        assertThat(result.midiNotes().getFirst().pitch()).isEqualTo(64);
        server.verify();
    }
}
