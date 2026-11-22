package glitched.adlips.adapter.in.web.opensource;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.auth.token-secret=opensource-query-test-secret-32-characters",
    "spring.datasource.url=jdbc:h2:mem:opensource_media;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "app.project.export-processing-enabled=false",
    "app.opensource.share-base-url=https://test.example/opensource",
    "app.opensource.playback-ttl-seconds=600"
})
class OpenSourceMediaIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessTokenPort tokens;
    @Autowired glitched.adlips.application.media.port.out.MediaContentStoragePort storage;
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();
    record Response(int status, JsonNode body) {}

    @BeforeEach void setup() {
        jdbc.update("INSERT INTO users(id,email,role,created_at) VALUES(96001,'query-owner@test.local','USER',CURRENT_TIMESTAMP),(96002,'query-viewer@test.local','USER',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO profiles(user_id,nickname,is_private,follower_count,following_count,is_muted) VALUES(96001,'작곡가',false,0,0,false)");
        jdbc.update("INSERT INTO media_files(id,owner_id,file_url,storage_key,file_type,status,file_size,created_at) VALUES(96101,96001,'http://local/private.wav','secret/audio','AUDIO','READY',100,CURRENT_TIMESTAMP),(96102,96001,'http://local/thumb.png','secret/thumb','IMAGE','READY',100,CURRENT_TIMESTAMP)");
        for(long id:List.of(96201L,96202L,96203L,96204L,96205L)) {
            jdbc.update("INSERT INTO open_sources(id,owner_id,title,description,media_file_id,thumbnail_media_file_id,is_public,created_at,updated_at,deleted_at) VALUES(?,96001,?,'Guitar riff',96101,96102,?,TIMESTAMP '2026-10-28 00:00:00',TIMESTAMP '2026-10-28 00:00:00',?)",id,id==96201?"100%_! 기타":"파트 "+id,id!=96204,id==96205?java.sql.Timestamp.valueOf("2026-10-28 01:00:00"):null);
        }
        jdbc.update("INSERT INTO open_sources(id,is_public) VALUES(96206,true)");
        for(long id:List.of(96201L,96204L,96205L,96299L)) {
            jdbc.update("INSERT INTO open_source_saves(user_id,open_source_id,saved_at) VALUES(96002,?,TIMESTAMP '2026-10-28 02:00:00')",id);
        }
    }
    @AfterEach void cleanup() {
        // Tables are introduced by this feature; clean only this fixture's rows.
        jdbc.update("DELETE FROM open_source_reactions");
        jdbc.update("DELETE FROM open_source_comments");
        jdbc.update("DELETE FROM open_source_saves WHERE user_id IN (96001,96002)");
        jdbc.update("DELETE FROM open_sources WHERE id BETWEEN 96201 AND 96299");
        jdbc.update("DELETE FROM media_files WHERE id IN (96101,96102)");
        jdbc.update("DELETE FROM profiles WHERE user_id=96001");
        jdbc.update("DELETE FROM users WHERE id IN (96001,96002)");
    }
    @Test void playbackAndShareRequireAvailableSourceAndAuthentication() throws Exception {
        for (String suffix : List.of("/playback", "/share")) {
            assertThat(call("GET", "/api/v1/open-sources/96201"+suffix, null, null).status()).isEqualTo(401);
            for(long id:List.of(96204L,96205L,96206L,96299L))
                assertThat(call("GET", "/api/v1/open-sources/"+id+suffix,null,96002L).status()).isEqualTo(404);
            assertThat(call("GET", "/api/v1/open-sources/0"+suffix,null,96002L).status()).isEqualTo(400);
        }
        var share=call("GET","/api/v1/open-sources/96201/share",null,96002L);
        assertThat(share.status()).isEqualTo(200);
        assertThat(share.body().at("/data/shareUrl").asText()).isEqualTo("https://test.example/opensource/96201");
    }
    @Test void unavailableAudioIsConflictAndStorageFailureIsUnavailable() throws Exception {
        jdbc.update("UPDATE media_files SET status='PROCESSING' WHERE id=96101");
        var pending=call("GET","/api/v1/open-sources/96201/playback",null,96002L);
        assertThat(pending.status()).isEqualTo(409);
        assertThat(pending.body().at("/errorCode").asText()).isEqualTo("OPEN_SOURCE_AUDIO_NOT_READY");
        jdbc.update("UPDATE media_files SET status='READY',storage_key='nonexistent-opensource-test.wav' WHERE id=96101");
        assertThat(call("GET","/api/v1/open-sources/96201/playback",null,96002L).status()).isEqualTo(503);
    }
    @Test void signedAudioSupportsRangesAndRejectsTamperingAndWithdrawal() throws Exception {
        storage.put("opensource-media-test/sample.wav", new byte[]{0,1,2,3,4,5,6,7}, "audio/wav");
        jdbc.update("UPDATE media_files SET storage_key='opensource-media-test/sample.wav',mime_type='audio/wav' WHERE id=96101");
        var playback=call("GET","/api/v1/open-sources/96201/playback",null,96002L);
        assertThat(playback.status()).isEqualTo(200);
        assertThat(playback.body().at("/data/durationMs").isNull()).isTrue();
        assertThat(playback.body().at("/data/mimeType").asText()).isEqualTo("audio/wav");
        URI issued=URI.create(playback.body().at("/data/audioUrl").asText());
        assertThat(issued.toString()).doesNotContain("sample.wav", "secret/audio", "private.wav");
        String path=issued.getRawPath()+"?"+issued.getRawQuery();
        var full=audio(path,null);
        assertThat(full.statusCode()).isEqualTo(200);
        assertThat(full.body()).containsExactly(0,1,2,3,4,5,6,7);
        assertThat(full.headers().firstValue("Cache-Control")).contains("no-store");
        var range=audio(path,"bytes=2-4");
        assertThat(range.statusCode()).isEqualTo(206);
        assertThat(range.body()).containsExactly(2,3,4);
        assertThat(range.headers().firstValue("Content-Range")).contains("bytes 2-4/8");
        assertThat(audio(path,"bytes=99-100").statusCode()).isEqualTo(416);
        assertThat(audio(path.replace("96201", "96202"),null).statusCode()).isEqualTo(403);
        assertThat(audio(path.replace("mediaId=96101", "mediaId=96102"),null).statusCode()).isEqualTo(403);
        assertThat(audio(path.replaceAll("signature=.*", "signature=invalid"),null).statusCode()).isEqualTo(403);
        assertThat(audio(path.replaceAll("expires=[0-9]+", "expires=1"),null).statusCode()).isEqualTo(403);
        assertThat(call("GET","/api/v1/open-sources/96201",null,96002L).body().at("/data/usageCount").asLong()).isZero();
        jdbc.update("UPDATE open_sources SET is_public=false WHERE id=96201");
        assertThat(audio(path,null).statusCode()).isEqualTo(404);
    }
    @Test void missingWrongOwnerAndNonAudioMediaCannotBePlayed() throws Exception {
        for(String change:List.of("owner_id=96002", "file_type='IMAGE'", "status='FAILED'")) {
            jdbc.update("UPDATE media_files SET owner_id=96001,file_type='AUDIO',status='READY' WHERE id=96101");
            jdbc.update("UPDATE media_files SET "+change+" WHERE id=96101");
            assertThat(call("GET","/api/v1/open-sources/96201/playback",null,96002L).status()).isEqualTo(409);
        }
        jdbc.update("UPDATE open_sources SET media_file_id=96999 WHERE id=96201");
        assertThat(call("GET","/api/v1/open-sources/96201/playback",null,96002L).status()).isEqualTo(409);
        jdbc.update("UPDATE users SET deleted_at=CURRENT_TIMESTAMP WHERE id=96001");
        assertThat(call("GET","/api/v1/open-sources/96201/share",null,96002L).status()).isEqualTo(404);
    }
    HttpResponse<byte[]> audio(String path,String range) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET();
        if(range!=null) builder.header("Range",range);
        return http.send(builder.build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    Response call(String method,String path,String body,Long user) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(20));
        if(user!=null) request.header("Authorization","Bearer "+tokens.issue(user));
        if(body!=null) request.header("Content-Type","application/json");
        var response=http.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(),json.readTree(response.body().isBlank()?"{}":response.body()));
    }
}
