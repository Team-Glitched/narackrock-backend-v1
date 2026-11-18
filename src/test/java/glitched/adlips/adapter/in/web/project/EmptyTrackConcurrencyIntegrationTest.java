package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import glitched.adlips.adapter.out.persistence.project.ProjectPersistenceAdapter;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.auth.token-secret=empty-track-concurrency-test-secret-32-characters",
    "spring.datasource.url=jdbc:h2:mem:empty_track_race;MODE=PostgreSQL;LOCK_TIMEOUT=15000",
    "app.project.export-processing-enabled=false"
})
class EmptyTrackConcurrencyIntegrationTest {
    private static final long TRACK_ID = 98401L;
    private static final String PROJECT = "/api/v1/projects/98301";
    private static final String TRACK = PROJECT + "/tracks/" + TRACK_ID;
    private static final String DELETE = "/api/v1/tracks/" + TRACK_ID + "/empty?projectId=98301";
    private static final String MIDI = """
            {"startTick":0,"durationTick":480,"midiNotes":[
                {"pitch":60,"startTick":0,"durationTick":240,"velocity":100}
            ]}
            """;

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessTokenPort tokens;
    @MockitoSpyBean ProjectPersistenceAdapter tracks;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void resetFixture() {
        jdbc.update("DELETE FROM editor_drafts WHERE project_id=98301");
        jdbc.update("DELETE FROM project_clips WHERE project_id=98301");
        jdbc.update("DELETE FROM project_tracks WHERE project_id=98301");
        jdbc.update("DELETE FROM project_members WHERE project_id=98301");
        jdbc.update("DELETE FROM projects WHERE id=98301");
        jdbc.update("DELETE FROM users WHERE id=98001");
        jdbc.update("INSERT INTO users(id,email,role,created_at) VALUES(98001,'track-race@test.local','USER',CURRENT_TIMESTAMP)");
        jdbc.update("""
                INSERT INTO projects(id,owner_id,title,album_image_file_id,bpm,time_signature_numerator,
                    time_signature_denominator,ppq,max_duration_ms,major_version,minor_version,status,
                    is_public,created_at,updated_at)
                VALUES(98301,98001,'곡',98999,120,4,4,480,60000,1,1,'IN_PROGRESS',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO project_members(id,project_id,user_id,role,joined_at) VALUES(98351,98301,98001,'OWNER',CURRENT_TIMESTAMP)");
        jdbc.update("""
                INSERT INTO project_tracks(id,project_id,owner_id,name,instrument,volume,pan,sort_order,
                    is_muted,is_solo,approval_status,is_deleted,created_at,updated_at)
                VALUES(98401,98301,98001,'피아노','piano',100,0,0,false,false,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void midiCreationFirstLeavesTheClipVisible() throws Exception {
        var results = race(request("POST", TRACK + "/clips/midi", MIDI), request("DELETE", DELETE, null));
        assertThat(results.first().statusCode()).isEqualTo(201);
        assertThat(results.second().statusCode()).isEqualTo(204);
        assertVisibleMidiClip();
    }

    @Test
    void emptyDeletionFirstRejectsMidiCreationWithoutOrphaningAClip() throws Exception {
        var results = race(request("DELETE", DELETE, null), request("POST", TRACK + "/clips/midi", MIDI));
        assertThat(results.first().statusCode()).isEqualTo(204);
        assertThat(results.second().statusCode()).isEqualTo(404);
        assertThat(json.readTree(results.second().body()).path("errorCode").asText()).isEqualTo("TRACK_NOT_FOUND");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM project_tracks WHERE id=?", Boolean.class, TRACK_ID)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM project_clips WHERE track_id=?", Integer.class, TRACK_ID)).isZero();
    }

    @Test
    void cleanupFirstPreservesAnActiveDraftForTheWaitingCommit() throws Exception {
        var draftId = UUID.randomUUID();
        var saved = http.send(request("PUT", TRACK + "/editor-drafts/" + draftId, """
                {"expectedRevision":0,"mutationId":"%s","content":%s}
                """.formatted(UUID.randomUUID(), MIDI)), HttpResponse.BodyHandlers.ofString());
        assertThat(saved.statusCode()).isEqualTo(200);

        var results = race(request("DELETE", DELETE, null),
                request("POST", PROJECT + "/editor-drafts/" + draftId + "/commit", "{\"expectedRevision\":1}"));
        assertThat(results.first().statusCode()).isEqualTo(204);
        assertThat(results.second().statusCode()).isEqualTo(200);
        assertVisibleMidiClip();
    }

    private void assertVisibleMidiClip() throws Exception {
        var response = http.send(request("GET", PROJECT + "/tracks", null), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var track = json.readTree(response.body()).at("/data/tracks/0");
        assertThat(track.path("trackId").asLong()).isEqualTo(TRACK_ID);
        assertThat(track.path("clips").size()).isEqualTo(1);
        assertThat(track.at("/clips/0/midiNotes/0/pitch").asInt()).isEqualTo(60);
    }

    private record RaceResult(HttpResponse<String> first, HttpResponse<String> second) {}

    private RaceResult race(HttpRequest first, HttpRequest second) throws Exception {
        var firstReachedTrack = new CountDownLatch(1);
        var resumeFirst = new CountDownLatch(1);
        var pauseOnce = new AtomicBoolean(true);
        // Pause the first request after its real track lookup, while the project write lock is held.
        doAnswer(invocation -> {
            var track = invocation.callRealMethod();
            if (pauseOnce.getAndSet(false)) {
                firstReachedTrack.countDown();
                assertThat(resumeFirst.await(12, TimeUnit.SECONDS)).isTrue();
            }
            return track;
        }).when(tracks).findTrackByIdAndIsDeletedFalse(TRACK_ID);

        var firstResponse = http.sendAsync(first, HttpResponse.BodyHandlers.ofString());
        CompletableFuture<HttpResponse<String>> secondResponse;
        try {
            assertThat(firstReachedTrack.await(8, TimeUnit.SECONDS))
                    .as("First request reached the track lookup. Response if completed: %s", firstResponse.getNow(null))
                    .isTrue();
            secondResponse = http.sendAsync(second, HttpResponse.BodyHandlers.ofString());
            awaitBlockedProjectLock();
            assertThat(firstResponse).isNotDone();
            assertThat(secondResponse).isNotDone();
        } finally {
            resumeFirst.countDown();
        }
        return new RaceResult(firstResponse.get(15, TimeUnit.SECONDS), secondResponse.get(15, TimeUnit.SECONDS));
    }

    private void awaitBlockedProjectLock() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        do {
            int blocked = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS
                    WHERE BLOCKER_ID IS NOT NULL AND UPPER(EXECUTING_STATEMENT) LIKE '%FROM PROJECTS%FOR UPDATE%'
                    """, Integer.class);
            if (blocked > 0) return;
            Thread.sleep(25);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("The second HTTP request never waited for the project row lock");
    }

    private HttpRequest request(String method, String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + tokens.issue(98001L))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
