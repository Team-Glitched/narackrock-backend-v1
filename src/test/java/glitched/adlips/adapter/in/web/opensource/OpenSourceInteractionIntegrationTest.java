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
    "spring.datasource.url=jdbc:h2:mem:opensource_interaction;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "app.project.export-processing-enabled=false"
})
class OpenSourceInteractionIntegrationTest {
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
        // Tables are introduced by this feature; clean only this fixture's rows.
        jdbc.update("DELETE FROM open_source_reactions");
        jdbc.update("DELETE FROM open_source_comments");
        jdbc.update("DELETE FROM open_source_saves WHERE user_id IN (96001,96002)");
        jdbc.update("DELETE FROM open_sources WHERE id BETWEEN 96201 AND 96299");
        jdbc.update("DELETE FROM media_files WHERE id IN (96101,96102)");
        jdbc.update("DELETE FROM profiles WHERE user_id=96001");
        jdbc.update("DELETE FROM users WHERE id IN (96001,96002)");
    }
    @Test void sourceReactionsToggleSwitchAndFeedUsesRealCounts() throws Exception {
        var like=call("POST","/api/v1/open-sources/96201/likes",null,96002L);
        assertThat(like.status()).isEqualTo(200);
        assertThat(like.body().at("/data/isLiked").asBoolean()).isTrue();
        assertThat(like.body().at("/data/likeCount").asLong()).isEqualTo(1);
        var dislike=call("POST","/api/v1/open-sources/96201/dislikes",null,96002L);
        assertThat(dislike.body().at("/data/isLiked").asBoolean()).isFalse();
        assertThat(dislike.body().at("/data/dislikeCount").asLong()).isEqualTo(1);
        var detail=call("GET","/api/v1/open-sources/96201",null,96002L);
        assertThat(detail.body().at("/data/isDisliked").asBoolean()).isTrue();
        assertThat(detail.body().at("/data/likeCount").asLong()).isZero();
        var saved=call("GET","/api/v1/users/me/open-sources",null,96002L);
        assertThat(saved.body().at("/data/items/3/source/dislikeCount").asLong()).isEqualTo(1);
        assertThat(call("POST","/api/v1/open-sources/96201/dislikes",null,96002L).body().at("/data/dislikeCount").asLong()).isZero();
    }
    @Test void commentsRepliesPaginationAndAggregateAreConsistent() throws Exception {
        long root=comment(96201,"  root  ",null);
        long second=comment(96201,"second",null);
        long reply=comment(96201,"reply",root);
        var list=call("GET","/api/v1/open-sources/96201/comments?size=1",null,96002L);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body().at("/data/totalCount").asLong()).isEqualTo(2);
        assertThat(list.body().at("/data/hasNext").asBoolean()).isTrue();
        assertThat(list.body().at("/data/comments/0/commentId").asLong()).isEqualTo(root);
        assertThat(list.body().at("/data/comments/0/content").asText()).isEqualTo("root");
        assertThat(list.body().at("/data/comments/0/replyCount").asLong()).isEqualTo(1);
        assertThat(list.body().at("/data/comments/0/parentCommentId").isNull()).isTrue();
        var replies=call("GET","/api/v1/open-sources/96201/comments?parentCommentId="+root,null,96002L);
        assertThat(replies.body().at("/data/size").asInt()).isEqualTo(10);
        assertThat(replies.body().at("/data/totalCount").asLong()).isEqualTo(1);
        assertThat(replies.body().at("/data/comments/0/commentId").asLong()).isEqualTo(reply);
        assertThat(call("GET","/api/v1/open-sources/96201",null,96002L).body().at("/data/commentCount").asLong()).isEqualTo(3);
        assertThat(call("GET","/api/v1/open-sources/96201/comments?page=1&size=1",null,96002L).body().at("/data/comments/0/commentId").asLong()).isEqualTo(second);
    }
    @Test void invalidCommentParentsAndContentAreRejected() throws Exception {
        long root=comment(96201,"root",null), reply=comment(96201,"reply",root);
        assertThat(postComment(96202,"foreign",root).status()).isEqualTo(404);
        assertThat(postComment(96201,"depth",reply).status()).isEqualTo(400);
        assertThat(postComment(96201,"missing",99999L).status()).isEqualTo(404);
        assertThat(postComment(96201,"   ",null).status()).isEqualTo(400);
        assertThat(postComment(96201,"a".repeat(1001),null).status()).isEqualTo(400);
        assertThat(postComment(96201,"a".repeat(1000),null).status()).isEqualTo(201);
        assertThat(call("POST","/api/v1/open-sources/96201/comments","{\"content\":\"test\",\"userId\":96001}",96002L).status()).isEqualTo(400);
        assertThat(call("GET","/api/v1/open-sources/96201/comments?parentCommentId="+reply,null,96002L).status()).isEqualTo(400);
        for(String query:List.of("?page=-1","?size=0","?size=101","?page=2147483647&size=100"))
            assertThat(call("GET","/api/v1/open-sources/96201/comments"+query,null,96002L).status()).isEqualTo(400);
    }
    @Test void commentReactionsAndSourceAccessAreEnforced() throws Exception {
        long root=comment(96201,"root",null);
        String likes="/api/v1/open-sources/comments/"+root+"/likes";
        assertThat(call("POST",likes,null,96002L).body().at("/data/likeCount").asLong()).isEqualTo(1);
        var comments=call("GET","/api/v1/open-sources/96201/comments",null,96002L);
        assertThat(comments.body().at("/data/comments/0/isLiked").asBoolean()).isTrue();
        assertThat(call("POST",likes,null,96002L).body().at("/data/likeCount").asLong()).isZero();
        assertThat(call("POST",likes.replace("likes","dislikes"),null,96001L).body().at("/data/dislikeCount").asLong()).isEqualTo(1);
        jdbc.update("UPDATE open_sources SET is_public=false WHERE id=96201");
        assertThat(call("POST",likes,null,96002L).status()).isEqualTo(404);
        assertThat(call("GET","/api/v1/open-sources/96201/comments",null,96002L).status()).isEqualTo(404);
        assertThat(postComment(96201,"hidden",null).status()).isEqualTo(404);
    }
    @Test void concurrentTogglesHaveNoDuplicatesOrLostTransitions() throws Exception {
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Callable<Response>> jobs=new ArrayList<>();
            for(int i=0;i<8;i++) jobs.add(() -> call("POST","/api/v1/open-sources/96201/likes",null,96002L));
            for(var result:pool.invokeAll(jobs)) assertThat(result.get().status()).isEqualTo(200);
        }
        assertThat(call("GET","/api/v1/open-sources/96201",null,96002L).body().at("/data/likeCount").asLong()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM open_source_reactions",Integer.class)).isZero();
    }
    @Test void privateMissingDeletedAndUnauthenticatedRequestsAreRejected() throws Exception {
        for(long id:List.of(96204L,96205L,96299L)) {
            assertThat(call("POST","/api/v1/open-sources/"+id+"/likes",null,96002L).status()).isEqualTo(404);
            assertThat(postComment(id,"test",null).status()).isEqualTo(404);
        }
        assertThat(call("POST","/api/v1/open-sources/96201/likes",null,null).status()).isEqualTo(401);
        assertThat(call("POST","/api/v1/open-sources/96201/comments","{\"content\":\"test\"}",null).status()).isEqualTo(401);
        assertThat(call("GET","/api/v1/open-sources/96201/comments",null,null).status()).isEqualTo(401);
    }
    @Test void hiddenRootAlsoHidesRepliesAndTheirReactions() throws Exception {
        long root=comment(96201,"root",null),reply=comment(96201,"reply",root);
        jdbc.update("UPDATE open_source_comments SET deleted_at=CURRENT_TIMESTAMP WHERE id=?",root);
        assertThat(call("GET","/api/v1/open-sources/96201/comments",null,96002L).body().at("/data/totalCount").asLong()).isZero();
        assertThat(call("GET","/api/v1/open-sources/96201",null,96002L).body().at("/data/commentCount").asLong()).isZero();
        assertThat(call("POST","/api/v1/open-sources/comments/"+reply+"/likes",null,96002L).status()).isEqualTo(404);
    }
    @Test void reactionsAreScopedToViewerAndTarget() throws Exception {
        long root=comment(96201,"root",null);
        call("POST","/api/v1/open-sources/96201/likes",null,96001L);
        call("POST","/api/v1/open-sources/comments/"+root+"/likes",null,96002L);
        var detail=call("GET","/api/v1/open-sources/96201",null,96002L);
        assertThat(detail.body().at("/data/isLiked").asBoolean()).isFalse();
        assertThat(detail.body().at("/data/likeCount").asLong()).isEqualTo(1);
        var other=call("GET","/api/v1/open-sources/96202",null,96002L);
        assertThat(other.body().at("/data/likeCount").asLong()).isZero();
    }
    long comment(long source,String content,Long parent) throws Exception {
        var result=postComment(source,content,parent);
        assertThat(result.status()).as(result.body().toString()).isEqualTo(201);
        return result.body().at("/data/comment/commentId").asLong();
    }
    Response postComment(long source,String content,Long parent) throws Exception {
        Map<String,Object> body=new HashMap<>(); body.put("content",content);body.put("parentCommentId",parent);
        return call("POST","/api/v1/open-sources/"+source+"/comments",json.writeValueAsString(body),96002L);
    }
    Response call(String method,String path,String body,Long user) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(20));
        if(user!=null) request.header("Authorization","Bearer "+tokens.issue(user));
        if(body!=null) request.header("Content-Type","application/json");
        var response=http.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(),json.readTree(response.body().isBlank()?"{}":response.body()));
    }
}
