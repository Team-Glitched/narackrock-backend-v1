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
    "spring.datasource.url=jdbc:h2:mem:opensource_query;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "app.project.export-processing-enabled=false"
})
class OpenSourceQueryIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessTokenPort tokens;
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
        jdbc.update("DELETE FROM open_source_views WHERE open_source_id BETWEEN 96201 AND 96299");
        jdbc.update("DELETE FROM open_source_reactions WHERE target_type='SOURCE' AND target_id BETWEEN 96201 AND 96299");
        jdbc.update("DELETE FROM open_source_saves WHERE user_id IN (96001,96002)");
        jdbc.update("DELETE FROM open_sources WHERE id BETWEEN 96201 AND 96299");
        jdbc.update("DELETE FROM media_files WHERE id IN (96101,96102)");
        jdbc.update("DELETE FROM profiles WHERE user_id=96001");
        jdbc.update("DELETE FROM users WHERE id IN (96001,96002)");
    }
    @Test void publicListHasStableOrderPaginationAndViewerSaveState() throws Exception {
        var first=get("/api/v1/open-sources?size=2&page=0",96002L);
        assertThat(first.status()).isEqualTo(200);
        var data=first.body().get("data");
        assertThat(data.get("hasNext").asBoolean()).isTrue();
        assertThat(ids(data.get("items"))).containsExactly(96203L,96202L);
        var last=get("/api/v1/open-sources?size=2&page=1",96002L).body().get("data");
        assertThat(ids(last.get("items"))).containsExactly(96201L);
        assertThat(last.get("hasNext").asBoolean()).isFalse();
        assertThat(last.at("/items/0/isSaved").asBoolean()).isTrue();
        assertThat(last.at("/items/0/canPreview").asBoolean()).isTrue();
        assertThat(last.at("/items/0/writer/nickname").asText()).isEqualTo("작곡가");
        assertThat(get("/api/v1/open-sources?page=2&size=2",96002L).body().at("/data/items").size()).isZero();
        assertThat(get("/api/v1/sources/96201", 96002L).body().at("/data/openSourceId").asLong()).isEqualTo(96201L);
        assertThat(ids(get("/api/v1/sources?size=2&page=0", 96002L).body().at("/data/items"))).containsExactly(96203L, 96202L);
    }
    @Test void popularSourcesUseTheSongScoreAndKeepVisibilityAndViewerState() throws Exception {
        jdbc.update("INSERT INTO open_source_reactions(user_id,target_type,target_id,reaction_type,created_at) "
                + "VALUES (96002,'SOURCE',96201,'LIKE',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO open_source_views(open_source_id,viewer_id,last_viewed_at,view_count) "
                + "VALUES (96202,96002,CURRENT_TIMESTAMP,5),(96204,96002,CURRENT_TIMESTAMP,100)");

        var response = get("/api/v1/open-sources/popular?size=2", 96002L);
        assertThat(response.status()).isEqualTo(200);
        var data = response.body().get("data");
        assertThat(ids(data.get("items"))).containsExactly(96202L, 96201L);
        assertThat(data.at("/items/1/isSaved").asBoolean()).isTrue();
        assertThat(data.at("/items/1/isLiked").asBoolean()).isTrue();
        assertThat(data.get("hasNext").asBoolean()).isFalse();
        assertThat(get("/api/v1/open-sources/popular?size=0", 96002L).status()).isEqualTo(400);
        assertThat(get("/api/v1/open-sources/popular", null).status()).isEqualTo(401);
    }

    @Test void sourcePlaybackCountsOnlyOncePerViewerWithinTheViewWindow() throws Exception {
        assertThat(postView(96201L, null).status()).isEqualTo(401);
        assertThat(postView(96204L, 96002L).status()).isEqualTo(404);
        var first = postView(96201L, 96002L);
        assertThat(first.status()).isEqualTo(202);
        assertThat(first.body().at("/data/counted").asBoolean()).isTrue();
        assertThat(postView(96201L, 96002L).body().at("/data/counted").asBoolean()).isFalse();
        assertThat(postView(96201L, 96001L).body().at("/data/counted").asBoolean()).isTrue();

        jdbc.update("UPDATE open_source_views SET last_viewed_at=TIMESTAMP '2000-01-01 00:00:00' "
                + "WHERE open_source_id=96201 AND viewer_id=96002");
        assertThat(postView(96201L, 96002L).body().at("/data/counted").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT SUM(view_count) FROM open_source_views WHERE open_source_id=96201",
                Long.class)).isEqualTo(3L);
        assertThat(ids(get("/api/v1/open-sources/popular?size=1", 96002L).body().at("/data/items")))
                .containsExactly(96201L);
    }

    @Test void searchIsCaseInsensitiveLiteralSubstringIncludingWildcards() throws Exception {
        for(String term:List.of("gUiTaR"," 기타 ","%","_","!")) {
            var result=get("/api/v1/open-sources?keyword="+URLEncoder.encode(term,StandardCharsets.UTF_8),96002L);
            assertThat(result.status()).isEqualTo(200);
            assertThat(result.body().at("/data/items").size()).isEqualTo(term.equals("gUiTaR")?3:1);
        }
        assertThat(get("/api/v1/open-sources?keyword=not-found",96002L).body().at("/data/items").size()).isZero();
        assertThat(get("/api/v1/open-sources?keyword=%20%20",96002L).body().at("/data/items").size()).isEqualTo(3);
    }
    @Test void detailIsFlatAndNeverExposesAudioOrStorageKey() throws Exception {
        var response=get("/api/v1/open-sources/96201",96002L);
        assertThat(response.status()).isEqualTo(200);
        var data=response.body().get("data");
        assertThat(data.get("openSourceId").asLong()).isEqualTo(96201L);
        assertThat(data.get("canPreview").asBoolean()).isTrue();
        assertThat(data.get("durationMs").isNull()).isTrue();
        assertThat(data.get("writer").get("profileImageUrl").isNull()).isTrue();
        assertThat(data.get("thumbnailUrl").asText()).isEqualTo("http://local/thumb.png");
        assertThat(data.toString()).doesNotContain("private.wav","secret/","mediaFileId");
        assertThat(data.get("usageCount").asLong()).isZero();
        assertThat(data.get("isLiked").asBoolean()).isFalse();
        for(long id:List.of(96204L,96205L,96206L,96299L)) assertThat(get("/api/v1/open-sources/"+id,96001L).status()).isEqualTo(404);
    }
    @Test void savedListRetainsUnavailableIdsButRedactsContentAndIsUserScoped() throws Exception {
        var result=get("/api/v1/users/me/open-sources",96002L);
        assertThat(result.status()).isEqualTo(200);
        var items=result.body().at("/data/items");
        assertThat(ids(items)).containsExactly(96299L,96205L,96204L,96201L);
        for(int index=0;index<3;index++) {
            assertThat(items.get(index).get("availability").asText()).isEqualTo("UNAVAILABLE");
            assertThat(items.get(index).get("source").isNull()).isTrue();
        }
        assertThat(items.get(3).at("/source/isSaved").asBoolean()).isTrue();
        assertThat(get("/api/v1/users/me/open-sources?userId=96002",96001L).body().at("/data/items").size()).isZero();
    }
    @Test void ownerPublicationsIncludePrivateButExcludeDeletedAndForeign() throws Exception {
        var result=get("/api/v1/users/me/open-source-publications?size=2",96001L);
        assertThat(result.status()).isEqualTo(200);
        assertThat(ids(result.body().at("/data/items"))).containsExactly(96204L,96203L);
        assertThat(result.body().at("/data/items/0/visibility").asText()).isEqualTo("PRIVATE");
        assertThat(result.body().at("/data/hasNext").asBoolean()).isTrue();
        assertThat(get("/api/v1/users/me/open-source-publications",96002L).body().at("/data/items").size()).isZero();
    }
    @Test void previewReflectsMediaReadinessAndInactiveAuthorIsHidden() throws Exception {
        jdbc.update("UPDATE media_files SET status='UPLOADING' WHERE id=96101");
        assertThat(get("/api/v1/open-sources/96201",96002L).body().at("/data/canPreview").asBoolean()).isFalse();
        jdbc.update("UPDATE users SET deleted_at=CURRENT_TIMESTAMP WHERE id=96001");
        assertThat(get("/api/v1/open-sources/96201",96002L).status()).isEqualTo(404);
        assertThat(get("/api/v1/open-sources",96002L).body().at("/data/items").size()).isZero();
        assertThat(get("/api/v1/users/me/open-sources",96002L).body().at("/data/items/3/source").isNull()).isTrue();
    }
    @Test void invalidParametersAndAuthenticationAreCheckedOnAllRoutes() throws Exception {
        for(String path:List.of("/api/v1/open-sources","/api/v1/users/me/open-sources","/api/v1/users/me/open-source-publications")) {
            for(String query:List.of("?page=-1","?size=0","?size=101","?page=2147483647&size=100","?size=abc")) {
                var result=get(path+query,96002L);
                assertThat(result.status()).as(path+query).isEqualTo(400);
                assertThat(result.body().get("errorCode").asText()).isEqualTo("INVALID_REQUEST");
            }
            assertThat(get(path,null).status()).isEqualTo(401);
        }
        assertThat(get("/api/v1/open-sources/96201",null).status()).isEqualTo(401);
        assertThat(get("/api/v1/open-sources/0",96002L).status()).isEqualTo(400);
        assertThat(get("/api/v1/open-sources?keyword="+"a".repeat(101),96002L).status()).isEqualTo(400);
    }
    @Test void timestampsTakePrecedenceOverIdAndSavedPaginationIncludesUnavailableItems() throws Exception {
        jdbc.update("UPDATE open_sources SET created_at=TIMESTAMP '2026-10-29 00:00:00' WHERE id=96201");
        assertThat(ids(get("/api/v1/open-sources?size=1",96002L).body().at("/data/items"))).containsExactly(96201L);
        assertThat(ids(get("/api/v1/users/me/open-source-publications?size=1",96001L).body().at("/data/items"))).containsExactly(96201L);
        jdbc.update("UPDATE open_source_saves SET saved_at=TIMESTAMP '2026-10-29 00:00:00' WHERE open_source_id=96201");
        var first=get("/api/v1/users/me/open-sources?size=2",96002L).body().get("data");
        assertThat(ids(first.get("items"))).containsExactly(96201L,96299L);
        assertThat(first.get("hasNext").asBoolean()).isTrue();
        var last=get("/api/v1/users/me/open-sources?page=1&size=2",96002L).body().get("data");
        assertThat(ids(last.get("items"))).containsExactly(96205L,96204L);
        assertThat(last.get("hasNext").asBoolean()).isFalse();
    }
    @Test void usageCountCountsDistinctActiveProjectsIgnoringDeletedAndRejected() throws Exception {
        try {
            jdbc.update("""
                    INSERT INTO projects(id,owner_id,title,album_image_file_id,bpm,time_signature_numerator,time_signature_denominator,ppq,max_duration_ms,major_version,minor_version,status,is_public,deleted_at,created_at,updated_at) VALUES
                    (96301,96002,'활성곡1',96102,120,4,4,480,60000,1,1,'IN_PROGRESS',true,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96302,96002,'활성곡2',96102,120,4,4,480,60000,1,1,'COMPLETED',true,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96303,96002,'삭제곡',96102,120,4,4,480,60000,1,1,'DELETED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """);
            jdbc.update("""
                    INSERT INTO project_tracks(id,project_id,owner_id,name,instrument,volume,pan,sort_order,is_muted,is_solo,approval_status,is_deleted,created_at,updated_at) VALUES
                    (96401,96301,96002,'기타1','acoustic-guitar',100,0,0,false,false,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96402,96301,96002,'기타2','acoustic-guitar',100,0,1,false,false,'APPROVED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96403,96302,96002,'반려트랙','piano',100,0,0,false,false,'REJECTED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96404,96302,96002,'유효트랙','piano',100,0,1,false,false,'APPROVED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96405,96303,96002,'삭제곡트랙','piano',100,0,0,false,false,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """);
            jdbc.update("""
                    INSERT INTO project_clips(id,project_id,track_id,owner_id,clip_type,source_type,origin_open_source_id,media_file_id,start_tick,duration_tick,start_time_ms,clip_offset_ms,duration_ms,sort_order,approval_status,is_deleted,created_at,updated_at) VALUES
                    (96501,96301,96401,96002,'AUDIO','IMPORTED',96201,96101,0,480,0,0,500,0,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96502,96301,96402,96002,'AUDIO','IMPORTED',96201,96101,0,480,0,0,500,0,'APPROVED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96503,96302,96403,96002,'AUDIO','IMPORTED',96201,96101,0,480,0,0,500,0,'APPROVED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96504,96302,96404,96002,'AUDIO','IMPORTED',96201,96101,0,480,0,0,500,0,'REJECTED',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96505,96302,96404,96002,'AUDIO','IMPORTED',96201,96101,480,480,500,0,500,1,'APPROVED',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                    (96506,96303,96405,96002,'AUDIO','IMPORTED',96201,96101,0,480,0,0,500,0,'DRAFT',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """);
            assertThat(get("/api/v1/open-sources/96201", 96002L).body().at("/data/usageCount").asLong()).isEqualTo(1L);
            assertThat(get("/api/v1/open-sources/96202", 96002L).body().at("/data/usageCount").asLong()).isZero();
        } finally {
            jdbc.update("DELETE FROM project_clips WHERE id BETWEEN 96501 AND 96599");
            jdbc.update("DELETE FROM project_tracks WHERE id BETWEEN 96401 AND 96499");
            jdbc.update("DELETE FROM projects WHERE id BETWEEN 96301 AND 96399");
        }
    }
    List<Long> ids(JsonNode items) {
        List<Long> result=new ArrayList<>(); items.forEach(item -> result.add(item.get("openSourceId").asLong())); return result;
    }
    Response get(String path,Long user) throws Exception {
        return request(path, user, "GET");
    }
    Response postView(Long id, Long user) throws Exception {
        return request("/api/v1/open-sources/" + id + "/views", user, "POST");
    }
    Response request(String path, Long user, String method) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(user!=null) request.header("Authorization","Bearer "+tokens.issue(user));
        var response=http.send(request.method(method, HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(),json.readTree(response.body().isBlank()?"{}":response.body()));
    }
}
