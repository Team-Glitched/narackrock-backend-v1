package glitched.adlips.adapter.in.web.project;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * POST /api/v1/projects/{projectId}/tracks/{trackId}/clips/open-source
 * 201 created; 400 invalid input/range; 401 unauthenticated; 403 not the caller's layer or completed project;
 * 404 private/deleted/missing open source (OPEN_SOURCE_NOT_FOUND); 409 source audio not ready (MEDIA_FILE_NOT_READY).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.auth.token-secret=opensource-clip-test-secret-32-characters",
    "spring.datasource.url=jdbc:h2:mem:opensource_clip;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "app.project.export-processing-enabled=false"
})
class OpenSourceClipIntegrationTest {
    private static final String CLIPS = "/api/v1/projects/97301/tracks/%d/clips/open-source";
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessTokenPort tokens;
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();
    record Response(int status, JsonNode body) {}

    @BeforeEach void setup() {
        // 97001 publishes the source; 97002 owns the project and track 97401; 97003 owns track 97402.
        jdbc.update("INSERT INTO users(id,email,role,created_at) VALUES(97001,'clip-source@test.local','USER',CURRENT_TIMESTAMP),(97002,'clip-owner@test.local','USER',CURRENT_TIMESTAMP),(97003,'clip-editor@test.local','USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO media_files(id,owner_id,file_url,storage_key,file_type,status,file_size,created_at) VALUES(97101,97001,'http://local/files/riff.wav','media/97001/riff.wav','AUDIO','READY',100,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO open_sources(id,owner_id,title,description,media_file_id,is_public,created_at,updated_at,deleted_at) VALUES"
                + "(97201,97001,'공개 리프','riff',97101,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL),"
                + "(97202,97001,'비공개 리프','riff',97101,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL),"
                + "(97203,97001,'삭제된 리프','riff',97101,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO projects(id,owner_id,title,album_image_file_id,bpm,time_signature_numerator,time_signature_denominator,ppq,max_duration_ms,major_version,minor_version,status,is_public,created_at,updated_at) VALUES(97301,97002,'곡',97999,120,4,4,480,60000,1,1,'IN_PROGRESS',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO project_members(id,project_id,user_id,role,joined_at) VALUES(97351,97301,97002,'OWNER',CURRENT_TIMESTAMP),(97352,97301,97003,'EDITOR',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO project_tracks(id,project_id,owner_id,name,instrument,volume,pan,sort_order,is_muted,is_solo,approval_status,is_deleted,created_at,updated_at) VALUES"
                + "(97401,97301,97002,'기타','acoustic-guitar',100,0,0,false,false,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
                + "(97402,97301,97003,'피아노','piano',100,0,1,false,false,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    }

    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM project_clips WHERE project_id=97301");
        jdbc.update("DELETE FROM project_tracks WHERE project_id=97301");
        jdbc.update("DELETE FROM project_members WHERE project_id=97301");
        jdbc.update("DELETE FROM projects WHERE id=97301");
        jdbc.update("DELETE FROM open_sources WHERE id BETWEEN 97201 AND 97299");
        jdbc.update("DELETE FROM media_files WHERE id=97101");
        jdbc.update("DELETE FROM users WHERE id IN (97001,97002,97003)");
    }

    @Test void insertsAnotherUsersPublicSourceAndCompositionReturnsPlayableMediaUrl() throws Exception {
        var created = call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":480,\"durationTick\":960}", 97002L);

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.body().at("/success").asBoolean()).isTrue();
        assertThat(created.body().at("/message").asText()).isEqualTo("오픈소스를 레이어에 추가했습니다.");
        JsonNode data = created.body().at("/data");
        assertThat(data.at("/projectId").asLong()).isEqualTo(97301L);
        assertThat(data.at("/trackId").asLong()).isEqualTo(97401L);
        assertThat(data.at("/clipType").asText()).isEqualTo("AUDIO");
        assertThat(data.at("/sourceType").asText()).isEqualTo("IMPORTED");
        assertThat(data.at("/mediaFileId").asLong()).isEqualTo(97101L);
        assertThat(data.at("/mediaUrl").asText()).isEqualTo("http://local/files/riff.wav");
        assertThat(data.at("/startTick").asInt()).isEqualTo(480);
        assertThat(data.at("/durationTick").asInt()).isEqualTo(960);
        assertThat(data.at("/startTimeMs").asInt()).isEqualTo(500);
        assertThat(data.at("/durationMs").asInt()).isEqualTo(1000);
        assertThat(data.at("/clipOffsetMs").asInt()).isZero();
        assertThat(data.at("/approvalStatus").asText()).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT owner_id FROM project_clips WHERE id=?", Long.class, data.at("/clipId").asLong()))
                .isEqualTo(97002L);

        var composition = call("GET", "/api/v1/projects/97301/tracks?view=WORKING", null, 97002L);
        assertThat(composition.status()).isEqualTo(200);
        JsonNode clip = composition.body().at("/data/tracks/0/clips/0");
        assertThat(composition.body().at("/data/tracks/0/trackId").asLong()).isEqualTo(97401L);
        assertThat(clip.at("/clipId").asLong()).isEqualTo(data.at("/clipId").asLong());
        assertThat(clip.at("/ownerId").asLong()).isEqualTo(97002L);
        assertThat(clip.at("/sourceType").asText()).isEqualTo("IMPORTED");
        assertThat(clip.at("/mediaFileId").asLong()).isEqualTo(97101L);
        assertThat(clip.at("/mediaUrl").asText()).isEqualTo("http://local/files/riff.wav");

        for (long viewer : new long[]{97002L, 97003L}) {
            var published = call("GET", "/api/v1/projects/97301/tracks", null, viewer);
            assertThat(published.status()).isEqualTo(200);
            assertThat(published.body().at("/data/tracks").size()).isZero();
        }
        var otherWorking = call("GET", "/api/v1/projects/97301/tracks?view=WORKING", null, 97003L);
        assertThat(otherWorking.body().at("/data/tracks/0/trackId").asLong()).isEqualTo(97402L);
        assertThat(otherWorking.body().at("/data/tracks/0/clips").size()).isZero();
    }

    @Test void rejectsAnotherMembersLayerAndCompletedProject() throws Exception {
        var notOwned = call("POST", CLIPS.formatted(97402), "{\"openSourceId\":97201,\"startTick\":0,\"durationTick\":480}", 97002L);
        assertThat(notOwned.status()).isEqualTo(403);
        assertThat(notOwned.body().at("/errorCode").asText()).isEqualTo("TRACK_EDIT_DENIED");

        jdbc.update("UPDATE projects SET status='COMPLETED' WHERE id=97301");
        var completed = call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":0,\"durationTick\":480}", 97002L);
        assertThat(completed.status()).isEqualTo(403);
        assertThat(completed.body().at("/errorCode").asText()).isEqualTo("PROJECT_ACCESS_DENIED");
        assertThat(clipCount()).isZero();
    }

    @Test void rejectsUnavailableSources() throws Exception {
        for (long id : new long[]{97202L, 97203L, 97299L}) {
            var response = call("POST", CLIPS.formatted(97401), "{\"openSourceId\":" + id + ",\"startTick\":0,\"durationTick\":480}", 97002L);
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.body().at("/errorCode").asText()).isEqualTo("OPEN_SOURCE_NOT_FOUND");
        }
        jdbc.update("UPDATE users SET deleted_at=CURRENT_TIMESTAMP WHERE id=97001");
        assertThat(call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":0,\"durationTick\":480}", 97002L).status())
                .isEqualTo(404);
        jdbc.update("UPDATE users SET deleted_at=NULL WHERE id=97001");
        jdbc.update("UPDATE media_files SET status='PROCESSING' WHERE id=97101");
        var notReady = call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":0,\"durationTick\":480}", 97002L);
        assertThat(notReady.status()).isEqualTo(409);
        assertThat(notReady.body().at("/errorCode").asText()).isEqualTo("MEDIA_FILE_NOT_READY");
        assertThat(clipCount()).isZero();
    }

    @Test void rejectsInvalidInputAndOutOfRange() throws Exception {
        var beyond = call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":57500,\"durationTick\":480}", 97002L);
        assertThat(beyond.status()).isEqualTo(400);
        assertThat(beyond.body().at("/errorCode").asText()).isEqualTo("PROJECT_DURATION_LIMIT_EXCEEDED");
        var empty = call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":0,\"durationTick\":0}", 97002L);
        assertThat(empty.status()).isEqualTo(400);
        assertThat(empty.body().at("/errorCode").asText()).isEqualTo("INVALID_CLIP_RANGE");
        var missingSource = call("POST", CLIPS.formatted(97401), "{\"startTick\":0,\"durationTick\":480}", 97002L);
        assertThat(missingSource.status()).isEqualTo(400);
        assertThat(missingSource.body().at("/errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(call("POST", CLIPS.formatted(97401), "{\"openSourceId\":97201,\"startTick\":0,\"durationTick\":480}", null).status())
                .isEqualTo(401);
        assertThat(clipCount()).isZero();
    }

    private int clipCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM project_clips WHERE project_id=97301", Integer.class);
    }

    private Response call(String method, String path, String body, Long user) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(java.time.Duration.ofSeconds(20));
        if (user != null) request.header("Authorization", "Bearer " + tokens.issue(user));
        if (body != null) request.header("Content-Type", "application/json");
        var response = http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), json.readTree(response.body().isBlank() ? "{}" : response.body()));
    }
}
