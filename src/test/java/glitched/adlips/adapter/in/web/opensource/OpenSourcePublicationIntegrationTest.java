package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.auth.token-secret=opensource-integration-test-secret-32-characters",
    "spring.datasource.url=jdbc:h2:mem:opensource_publication;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "app.project.export-processing-enabled=false"
})
class OpenSourcePublicationIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessTokenPort tokens;
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();
    String owner, other;
    record Response(int status, JsonNode body) {}

    @BeforeEach void setup() {
        jdbc.update("INSERT INTO users(id,email,role,created_at) VALUES (97001,'os-owner@test.local','USER',CURRENT_TIMESTAMP),(97002,'os-other@test.local','USER',CURRENT_TIMESTAMP)");
        for (long id : List.of(97101L, 97102L, 97103L)) {
            jdbc.update("INSERT INTO media_files(id,owner_id,file_url,storage_key,file_type,status,file_size,created_at) VALUES(?,97001,'http://local/audio','private/storage/key',?,'READY',100,CURRENT_TIMESTAMP)", id, id == 97103L ? "IMAGE" : "AUDIO");
        }
        owner = tokens.issue(97001L); other = tokens.issue(97002L);
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM open_source_saves WHERE open_source_id IN (SELECT id FROM open_sources WHERE owner_id=97001)");
        jdbc.update("DELETE FROM open_sources WHERE owner_id=97001");
        jdbc.update("DELETE FROM media_files WHERE id BETWEEN 97101 AND 97103");
        jdbc.update("DELETE FROM users WHERE id IN (97001,97002)");
    }
    @Test void createPrivateReadOwnerAndPublishSaveWithdrawRepublishDelete() throws Exception {
        long id = create();
        var detail = call("GET", manage(id), null, owner);
        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.body().at("/data/visibility").asText()).isEqualTo("PRIVATE");
        assertThat(detail.body().toString()).doesNotContain("private/storage", "http://local/audio");
        assertThat(call("PUT", path(id)+"/save", null, other).status()).isEqualTo(404);
        assertThat(call("PUT", path(id)+"/visibility", "{\"visibility\":\"PUBLIC\"}", owner).status()).isEqualTo(200);
        assertThat(call("PUT", path(id)+"/save", null, other).status()).isEqualTo(200);
        var firstTime = jdbc.queryForObject("SELECT updated_at FROM open_sources WHERE id=?", Object.class, id);
        call("PUT", path(id)+"/visibility", "{\"visibility\":\"PUBLIC\"}", owner);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM open_sources WHERE id=?", Object.class, id)).isEqualTo(firstTime);
        assertThat(call("PUT", path(id)+"/visibility", "{\"visibility\":\"PRIVATE\"}", owner).status()).isEqualTo(200);
        assertThat(call("PUT", path(id)+"/save", null, other).status()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM open_source_saves WHERE open_source_id=?", Integer.class, id)).isEqualTo(1);
        call("PUT", path(id)+"/visibility", "{\"visibility\":\"PUBLIC\"}", owner);
        assertThat(call("PUT", path(id)+"/save", null, other).status()).isEqualTo(200);
        var deleted = call("DELETE", path(id), null, owner);
        assertThat(deleted.status()).isEqualTo(200);
        assertThat(deleted.body().at("/data/isDeleted").asBoolean()).isTrue();
        assertThat(call("DELETE", path(id), null, owner).status()).isEqualTo(200);
        assertThat(call("GET", manage(id), null, owner).status()).isEqualTo(404);
        assertThat(call("PUT", path(id)+"/visibility", "{\"visibility\":\"PUBLIC\"}", owner).status()).isEqualTo(404);
        assertThat(call("DELETE", path(id)+"/save", null, other).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_files WHERE id=97101", Integer.class)).isEqualTo(1);
    }
    @Test void patchDistinguishesNullAndOmittedFields() throws Exception {
        long id = create();
        var response = call("PATCH", path(id), "{\"title\":\" new title \"}", owner);
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().at("/data/title").asText()).isEqualTo("new title");
        assertThat(response.body().at("/data/description").asText()).isEqualTo("desc");
        call("PATCH", path(id), "{\"description\":null,\"thumbnailMediaFileId\":null}", owner);
        assertThat(jdbc.queryForObject("SELECT description FROM open_sources WHERE id=?", String.class,id)).isNull();
        assertThat(jdbc.queryForObject("SELECT thumbnail_media_file_id FROM open_sources WHERE id=?", Long.class,id)).isNull();
        assertThat(jdbc.queryForObject("SELECT media_file_id FROM open_sources WHERE id=?", Long.class,id)).isEqualTo(97101);
    }
    @Test void patchRejectsUnknownImmutableEmptyAndMalformedFields() throws Exception {
        long id = create();
        for (String body : List.of("{}", "{\"mediaFileId\":97102}", "{\"ownerId\":97002}", "{\"title\":null}", "{\"title\":123}", "{\"thumbnailMediaFileId\":1.5}", "{\"thumbnailMediaFileId\":\"97103\"}", "{", "null")) {
            var response = call("PATCH", path(id), body, owner);
            assertThat(response.status()).as(body).isEqualTo(400);
            assertThat(response.body().at("/errorCode").asText()).isEqualTo("INVALID_REQUEST");
        }
    }
    @Test void privateOwnerBoundariesAndAuthenticationAreEnforced() throws Exception {
        long id = create();
        for (String method : List.of("PATCH", "DELETE")) {
            assertThat(call(method,path(id),method.equals("PATCH") ? "{\"title\":\"hack\"}" : null,other).status()).isEqualTo(404);
        }
        assertThat(call("GET",manage(id),null,other).status()).isEqualTo(404);
        assertThat(call("PUT",path(id)+"/visibility","{\"visibility\":\"PUBLIC\"}",other).status()).isEqualTo(404);
        assertThat(call("POST","/api/v1/open-sources",createBody(),null).status()).isEqualTo(401);
        assertThat(call("GET",manage(id),null,null).status()).isEqualTo(401);
    }
    @Test void ownershipReadinessAndTypeAreValidatedForCreationAndPublication() throws Exception {
        assertThat(call("POST","/api/v1/open-sources",createBody(),other).status()).isEqualTo(404);
        long id = create();
        jdbc.update("UPDATE media_files SET status='UPLOADING' WHERE id=97101");
        assertThat(call("POST","/api/v1/open-sources",createBody(),owner).status()).isEqualTo(409);
        assertThat(call("PUT",path(id)+"/visibility","{\"visibility\":\"PUBLIC\"}",owner).status()).isEqualTo(409);
        jdbc.update("UPDATE media_files SET status='READY',file_type='IMAGE' WHERE id=97101");
        assertThat(call("POST","/api/v1/open-sources",createBody(),owner).status()).isEqualTo(400);
        assertThat(call("PUT",path(id)+"/visibility","{\"visibility\":\"UNKNOWN\"}",owner).status()).isEqualTo(400);
    }
    @Test void concurrentDisjointPatchesDoNotLoseUpdates() throws Exception {
        long id=create();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(() -> call("PATCH",path(id),"{\"title\":\"changed\"}",owner));
            var second=pool.submit(() -> call("PATCH",path(id),"{\"description\":\"changed description\"}",owner));
            assertThat(first.get(10,TimeUnit.SECONDS).status()).isEqualTo(200);
            assertThat(second.get(10,TimeUnit.SECONDS).status()).isEqualTo(200);
        }
        var result=call("GET",manage(id),null,owner);
        assertThat(result.body().at("/data/title").asText()).isEqualTo("changed");
        assertThat(result.body().at("/data/description").asText()).isEqualTo("changed description");
    }
    @Test void incompleteLegacyRecordsCannotBeSaved() throws Exception {
        jdbc.update("INSERT INTO open_sources(id,is_public) VALUES(97999,true)");
        try {
            assertThat(call("PUT",path(97999)+"/save",null,other).status()).isEqualTo(404);
            assertThat(call("GET",manage(97999),null,owner).status()).isEqualTo(404);
        } finally { jdbc.update("DELETE FROM open_sources WHERE id=97999"); }
    }
    @Test void invalidThumbnailAndInactiveOwnerAreRejected() throws Exception {
        long id=create();
        assertThat(call("PATCH",path(id),"{\"thumbnailMediaFileId\":97102}",owner).status()).isEqualTo(400);
        jdbc.update("UPDATE media_files SET owner_id=97002 WHERE id=97103");
        assertThat(call("PATCH",path(id),"{\"thumbnailMediaFileId\":97103}",owner).status()).isEqualTo(404);
        jdbc.update("UPDATE users SET deleted_at=CURRENT_TIMESTAMP WHERE id=97001");
        assertThat(call("PUT",path(id)+"/visibility","{\"visibility\":\"PUBLIC\"}",owner).status()).isEqualTo(401);
    }
    @Test void creationRejectsMissingRequiredFieldsAndOwnerOverride() throws Exception {
        for(String body: List.of("{\"title\":\"x\"}", "{\"mediaFileId\":97101}", "{\"title\":\"x\",\"mediaFileId\":97101,\"ownerId\":97002}")) {
            assertThat(call("POST","/api/v1/open-sources",body,owner).status()).isEqualTo(400);
        }
    }
    @Test void selectedInstrumentSurvivesPersistenceAndEverySourceRead() throws Exception {
        var created = call("POST", "/api/v1/sources", createBody().replace("\"title\"", "\"instrument\":\" GUITAR \",\"title\""), owner);
        assertThat(created.status()).isEqualTo(201);
        long id = created.body().at("/data/openSourceId").asLong();
        assertThat(created.body().at("/data/instrument").asText()).isEqualTo("acoustic-guitar");
        assertThat(jdbc.queryForObject("SELECT instrument FROM open_sources WHERE id=?", String.class, id)).isEqualTo("acoustic-guitar");
        assertThat(call("GET", manage(id), null, owner).body().at("/data/instrument").asText()).isEqualTo("acoustic-guitar");
        assertThat(call("GET", "/api/v1/users/me/source-publications", null, owner).body().at("/data/items/0/instrument").asText()).isEqualTo("acoustic-guitar");
        call("PUT", path(id)+"/visibility", "{\"visibility\":\"PUBLIC\"}", owner);
        assertThat(call("GET", "/api/v1/sources/"+id, null, other).body().at("/data/instrument").asText()).isEqualTo("acoustic-guitar");
        assertThat(call("GET", "/api/v1/sources", null, other).body().at("/data/items/0/instrument").asText()).isEqualTo("acoustic-guitar");
        assertThat(call("GET", "/api/v1/sources/popular", null, other).body().at("/data/items/0/instrument").asText()).isEqualTo("acoustic-guitar");
        call("PUT", path(id)+"/save", null, other);
        assertThat(call("GET", "/api/v1/users/me/sources", null, other).body().at("/data/items/0/source/instrument").asText()).isEqualTo("acoustic-guitar");
        call("PUT", path(id)+"/visibility", "{\"visibility\":\"PRIVATE\"}", owner);
        assertThat(call("GET", manage(id), null, owner).body().at("/data/instrument").asText()).isEqualTo("acoustic-guitar");
    }

    @Test void instrumentUpdatesPreserveOmittedValuesAndSupportLegacyUnknownParts() throws Exception {
        long id = create();
        assertThat(call("GET", manage(id), null, owner).body().at("/data/instrument").isNull()).isTrue();
        for (String instrument : List.of("violin", "drum-kit", "vocal", "backing-vocal", "rap", "percussion", "other")) {
            var changed = call("PATCH", path(id), "{\"instrument\":\""+instrument+"\"}", owner);
            assertThat(changed.status()).isEqualTo(200);
            assertThat(changed.body().at("/data/instrument").asText()).isEqualTo(instrument);
        }
        call("PATCH", path(id), "{\"title\":\"renamed\"}", owner);
        assertThat(call("GET", manage(id), null, owner).body().at("/data/instrument").asText()).isEqualTo("other");
        assertThat(call("PATCH", path(id), "{\"instrument\":null}", owner).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT instrument FROM open_sources WHERE id=?", String.class, id)).isNull();
        assertThat(call("PATCH", path(id), "{\"instrument\":\"piano\"}", other).status()).isEqualTo(404);
    }

    @Test void instrumentOnlyPatchKeepsOverlongLegacyTextButNewDescriptionIsLimited() throws Exception {
        long id = create();
        String legacyTitle = "t".repeat(80), legacyDescription = "d".repeat(1200);
        jdbc.update("UPDATE open_sources SET title=?, description=? WHERE id=?", legacyTitle, legacyDescription, id);
        var changed = call("PATCH", path(id), "{\"instrument\":\"violin\"}", owner);
        assertThat(changed.status()).as(changed.body().toString()).isEqualTo(200);
        assertThat(changed.body().at("/data/instrument").asText()).isEqualTo("violin");
        assertThat(changed.body().at("/data/title").asText()).isEqualTo(legacyTitle);
        assertThat(changed.body().at("/data/description").asText()).isEqualTo(legacyDescription);
        assertThat(call("PATCH", path(id), "{\"description\":\""+"x".repeat(501)+"\"}", owner).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT description FROM open_sources WHERE id=?", String.class, id)).isEqualTo(legacyDescription);
        assertThat(jdbc.queryForObject("SELECT title FROM open_sources WHERE id=?", String.class, id)).isEqualTo(legacyTitle);
    }

    @Test void invalidInstrumentValuesAreRejectedBeforeCreationOrMutation() throws Exception {
        long id = create();
        for (String value : List.of("\"unknown\"", "\" \"", "123", "{}", "[]")) {
            String field = "\"instrument\":" + value;
            var created = call("POST", "/api/v1/sources", createBody().replace("\"title\"", field+",\"title\""), owner);
            assertThat(created.status()).as(value).isEqualTo(400);
            var patched = call("PATCH", path(id), "{"+field+"}", owner);
            assertThat(patched.status()).as(value).isEqualTo(400);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM open_sources WHERE owner_id=97001", Integer.class)).isEqualTo(1);
    }

    @Test void optionalMidiNotesArePersistedAndExposedOnlyInDetailWhileSummaryHasFlag() throws Exception {
        String midiBody = """
                {
                  "title": "MIDI 피아노",
                  "description": "코드 진행",
                  "mediaFileId": 97101,
                  "instrument": "piano",
                  "midiPpq": 480,
                  "midiBpm": 115,
                  "midiNotes": [
                    {"pitch": 60, "startTick": 0, "durationTick": 480, "velocity": 100},
                    {"pitch": 64, "startTick": 480, "durationTick": 240, "velocity": 90}
                  ]
                }
                """;
        var midiCreated = call("POST", "/api/v1/sources", midiBody, owner);
        assertThat(midiCreated.status()).as(midiCreated.body().toString()).isEqualTo(201);
        assertThat(midiCreated.body().at("/data/hasMidiNotes").asBoolean()).isTrue();
        long midiId = midiCreated.body().at("/data/openSourceId").asLong();

        var audioCreated = call("POST", "/api/v1/sources", createBody(), owner);
        assertThat(audioCreated.status()).isEqualTo(201);
        assertThat(audioCreated.body().at("/data/hasMidiNotes").asBoolean()).isFalse();
        long audioId = audioCreated.body().at("/data/openSourceId").asLong();

        call("PUT", path(midiId) + "/visibility", "{\"visibility\":\"PUBLIC\"}", owner);
        call("PUT", path(audioId) + "/visibility", "{\"visibility\":\"PUBLIC\"}", owner);

        var list = call("GET", "/api/v1/sources", null, other);
        assertThat(list.status()).isEqualTo(200);
        var items = list.body().at("/data/items");
        assertThat(items.get(0).at("/hasMidiNotes").asBoolean()).isFalse();
        assertThat(items.get(1).at("/hasMidiNotes").asBoolean()).isTrue();

        var midiDetail = call("GET", "/api/v1/sources/" + midiId, null, other);
        assertThat(midiDetail.status()).isEqualTo(200);
        assertThat(midiDetail.body().at("/data/hasMidiNotes").asBoolean()).isTrue();
        assertThat(midiDetail.body().at("/data/midiPpq").asInt()).isEqualTo(480);
        assertThat(midiDetail.body().at("/data/midiBpm").asInt()).isEqualTo(115);
        assertThat(midiDetail.body().at("/data/midiNotes")).hasSize(2);
        assertThat(midiDetail.body().at("/data/midiNotes/0/pitch").asInt()).isEqualTo(60);

        var audioDetail = call("GET", "/api/v1/sources/" + audioId, null, other);
        assertThat(audioDetail.status()).isEqualTo(200);
        assertThat(audioDetail.body().at("/data/hasMidiNotes").asBoolean()).isFalse();
        assertThat(audioDetail.body().at("/data/midiNotes").isNull()).isTrue();

        for (String badBody : List.of(
                createBody().replace("}", ",\"midiNotes\":[]}"),
                createBody().replace("}", ",\"midiPpq\":480}"),
                createBody().replace("}", ",\"midiNotes\":[{\"pitch\":128,\"startTick\":0,\"durationTick\":480,\"velocity\":100}]}"),
                createBody().replace("}", ",\"midiNotes\":[{\"pitch\":60,\"startTick\":0,\"durationTick\":0,\"velocity\":100}]}"))) {
            assertThat(call("POST", "/api/v1/sources", badBody, owner).status()).as(badBody).isEqualTo(400);
        }
    }

    long create() throws Exception {
        var response=call("POST","/api/v1/open-sources",createBody(),owner);
        assertThat(response.status()).as(response.body().toString()).isEqualTo(201);
        return response.body().at("/data/openSourceId").asLong();
    }
    String createBody() { return "{\"title\":\" title \",\"description\":\"desc\",\"mediaFileId\":97101,\"thumbnailMediaFileId\":97103}"; }
    String path(long id) { return "/api/v1/open-sources/"+id; }
    String manage(long id) { return "/api/v1/users/me/open-source-publications/"+id; }
    Response call(String method,String path,String body,String token) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(15));
        if(token!=null) builder.header("Authorization","Bearer "+token);
        if(body!=null) builder.header("Content-Type","application/json");
        var response=http.send(builder.method(method,body==null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(),json.readTree(response.body().isBlank()?"{}":response.body()));
    }
}
