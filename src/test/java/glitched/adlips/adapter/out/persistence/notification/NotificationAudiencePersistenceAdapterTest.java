package glitched.adlips.adapter.out.persistence.notification;
import static org.assertj.core.api.Assertions.assertThat;
import glitched.adlips.application.notification.*;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.notification.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
@DataJpaTest @Import({NotificationPersistenceAdapter.class,NotificationAudiencePersistenceAdapter.class})
class NotificationAudiencePersistenceAdapterTest {
    @Autowired NotificationPersistenceAdapter store;
    @Autowired NotificationAudiencePersistenceAdapter audience;
    @Autowired JdbcTemplate jdbc;
    NotificationActivityService service;
    @BeforeEach void setUp(){
        for(int id=1;id<=4;id++)jdbc.update("insert into users(id,email,role,created_at) values (?,?,'USER',CURRENT_TIMESTAMP)",id,"notification"+id+"@test.local");
        jdbc.update("insert into galleries(id,name,created_at) values(1,'알림 커뮤니티',CURRENT_TIMESTAMP)");
        jdbc.update("insert into posts(id,gallery_id,user_id,title,content,view_count,like_count,dislike_count,comment_count,created_at) values(1,1,1,'연주 이야기','내용',0,0,0,0,CURRENT_TIMESTAMP)");
        jdbc.update("insert into post_comments(id,post_id,user_id,content,reply_count,created_at) values(1,1,3,'원 댓글',0,CURRENT_TIMESTAMP)");
        jdbc.update("insert into post_comments(id,post_id,user_id,content,reply_count,parent_comment_id,created_at) values(2,1,2,'답글',0,1,CURRENT_TIMESTAMP)");
        service=new NotificationActivityService(store,audience,user->{},Clock.systemUTC());
    }
    @Test void replyNotifiesParentWriterOnceAndOwnLikesDoNotNotify(){
        service.record(NotificationType.POST_REPLY,2L,2L);
        service.record(NotificationType.POST_REPLY,2L,2L);
        service.record(NotificationType.POST_LIKED,1L,1L);
        assertThat(store.list(3L,null,null,false,20)).hasSize(1);
        assertThat(store.list(1L,null,null,false,20)).isEmpty();
        assertThat(store.list(4L,null,null,false,20)).isEmpty();
    }
    @Test void disabledReactionsAndOptionalCommunityPostsAreRespected(){
        store.preferences(1L,new NotificationPreferences.Values(false,false,false,false));
        service.record(NotificationType.POST_LIKED,2L,1L);
        jdbc.update("insert into gallery_subs(id,user_id,gallery_id,created_at) values(1,3,1,CURRENT_TIMESTAMP)");
        service.record(NotificationType.GALLERY_POST_PUBLISHED,2L,1L);
        assertThat(store.unread(1L)).isZero();assertThat(store.unread(3L)).isZero();
        store.preferences(3L,new NotificationPreferences.Values(true,false,true,false));
        service.record(NotificationType.GALLERY_POST_PUBLISHED,2L,1L);
        assertThat(store.unread(3L)).isEqualTo(1);
    }
    @Test void deletedContentIsRedactedWhenHistoryIsRead(){
        service.record(NotificationType.POST_LIKED,2L,1L);
        jdbc.update("update posts set deleted_at=CURRENT_TIMESTAMP where id=1");
        var inbox=new NotificationInboxUseCase(store,audience,TransactionRunner.direct(),user->{},Clock.systemUTC());
        var item=inbox.list(1L,null,null,false,20).items().getFirst();
        assertThat(item.available()).isFalse();assertThat(item.path()).isNull();assertThat(item.body()).doesNotContain("연주 이야기");
    }
    @Test void followRequestsRemainAsClosedHistoryAfterTheyAreHandled(){
        jdbc.update("insert into follow_requests(id,requester_id,target_id,created_at) values(1,2,1,CURRENT_TIMESTAMP)");
        service.record(NotificationType.FOLLOW_REQUESTED,2L,1L,1L,null);
        var n=store.list(1L,null,null,false,20).getFirst();
        assertThat(audience.access(1L,n).requestStatus()).isEqualTo("PENDING");
        jdbc.update("delete from follow_requests where id=1");
        assertThat(audience.access(1L,n).requestStatus()).isEqualTo("CLOSED");
        service.record(NotificationType.FOLLOW_ACCEPTED,1L,2L,1L,null);
        assertThat(store.unread(2L)).isEqualTo(1);
        assertThat(audience.access(1L,n).requestStatus()).isEqualTo("APPROVED");
    }
}
