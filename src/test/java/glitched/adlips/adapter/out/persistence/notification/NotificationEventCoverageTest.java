package glitched.adlips.adapter.out.persistence.notification;
import static org.assertj.core.api.Assertions.assertThat;
import glitched.adlips.application.notification.*;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.notification.*;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.shorts.*;
import glitched.adlips.domain.community.*;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.admin.UserBan;
import glitched.adlips.domain.report.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
@DataJpaTest @Import({NotificationAudiencePersistenceAdapter.class,NotificationPersistenceAdapter.class})
class NotificationEventCoverageTest {
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationAudiencePersistenceAdapter audience;
    @Autowired NotificationPersistenceAdapter store;
    Long projectId,shortId,contributionId,proposalId,banId,userBanId,reportId,clipId,postId,postCommentId,postReplyId,shortCommentId,shortReplyId;
    @BeforeEach void seed(){
        for(int i=1;i<=4;i++)jdbc.update("insert into users(id,email,role,created_at) values(?,?,?,CURRENT_TIMESTAMP)",i,"coverage"+i+"@test.local",i==2?"ADMIN":"USER");
        User owner=em.find(User.class,1L),admin=em.find(User.class,2L),approved=em.find(User.class,3L),visitor=em.find(User.class,4L);
        Gallery gallery=Gallery.create("알림 커뮤니티","설명");em.persist(gallery);
        Post post=new Post(gallery,owner,"연주 이야기","내용");em.persist(post);postId=post.getId();
        PostComment comment=new PostComment(post,approved,"댓글",null);em.persist(comment);postCommentId=comment.getId();
        PostComment reply=new PostComment(post,visitor,"답글",comment);em.persist(reply);postReplyId=reply.getId();
        Project project=new Project(owner,"알림 음악",null,701L);em.persist(project);project.startPublishing();project.publish(LocalDateTime.now());projectId=project.getId();
        em.persist(new ProjectMember(project,owner,ProjectMemberRole.OWNER));em.persist(new ProjectMember(project,approved,ProjectMemberRole.EDITOR));em.persist(new ProjectMember(project,visitor,ProjectMemberRole.EDITOR));
        ProjectContribution c=new ProjectContribution(project,approved,"기타 추가");c.approve(owner,null,LocalDateTime.now());em.persist(c);contributionId=c.getId();
        em.persist(new ProjectContribution(project,visitor,"대기 요청"));
        ShortForm shortForm=ShortForm.fromExport(project,1L,801L);em.persist(shortForm);shortId=shortForm.getId();
        ShortComment sc=new ShortComment(shortForm,approved,"음악 댓글",null);em.persist(sc);shortCommentId=sc.getId();
        ShortComment sr=new ShortComment(shortForm,visitor,"음악 답글",sc);em.persist(sr);shortReplyId=sr.getId();
        GalleryProposal proposal=GalleryProposal.submit(approved,"새 커뮤니티","개설 사유");em.persist(proposal);proposalId=proposal.getId();
        GalleryBan ban=GalleryBan.suspend(gallery,approved,admin,"이용 규정",7,LocalDateTime.now());em.persist(ban);banId=ban.getId();
        UserBan ub=UserBan.create(approved,admin,"서비스 규정",LocalDateTime.now().plusDays(7));em.persist(ub);userBanId=ub.getId();
        Report report=new Report(approved,ReportTargetType.POST,post.getId(),"SPAM","신고 내용");em.persist(report);reportId=report.getId();
        ProjectTrack track=new ProjectTrack(project,owner,"기타","acoustic-guitar",0);em.persist(track);
        ProjectClip clip=ProjectClip.createAudio(project,track,owner,ClipSourceType.IMPORTED,801L,null,0,960,0,500,0);clip.attributeOpenSource(1L);em.persist(clip);clipId=clip.getId();
        em.flush();
        jdbc.update("insert into open_sources(id,owner_id,title,media_file_id,is_public,created_at,updated_at) values(1,3,'기타 소스',801,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("insert into open_source_comments(id,open_source_id,user_id,content,created_at) values(1,1,3,'댓글',CURRENT_TIMESTAMP)");
        jdbc.update("insert into open_source_comments(id,open_source_id,user_id,parent_comment_id,content,created_at) values(2,1,4,1,'답글',CURRENT_TIMESTAMP)");
        jdbc.update("insert into follows(id,follower_id,following_id,created_at) values(1,3,1,CURRENT_TIMESTAMP),(2,4,3,CURRENT_TIMESTAMP)");
        jdbc.update("insert into gallery_subs(id,user_id,gallery_id,created_at) values(1,3,?,CURRENT_TIMESTAMP)",gallery.getId());
        jdbc.update("insert into follow_requests(id,requester_id,target_id,created_at) values(1,4,3,CURRENT_TIMESTAMP)");
        jdbc.update("update project_tracks set approval_status='APPROVED' where id=?",track.getId());
        jdbc.update("update project_clips set approval_status='APPROVED' where id=?",clipId);
    }
    @Test void everyNonChatEventHasAnAudienceAndAnAccessibleHistoryEntry(){
        for(NotificationType type:NotificationType.values()){
            Long target=switch(type){
                case FOLLOW_CREATED,FOLLOW_REQUESTED,FOLLOW_ACCEPTED,FOLLOW_REJECTED->3L;
                case CONTRIBUTION_CREATED,CONTRIBUTION_ACCEPTED,CONTRIBUTION_REJECTED->contributionId;
                case PROJECT_PUBLISHED,PROJECT_REFRESHED,FOLLOWING_SHORT_PUBLISHED,SOURCE_USED_IN_SHORT,SHORT_LIKED->shortId;
                case EXPORT_FAILED,PROJECT_COMPLETED,PROJECT_DELETED->projectId;
                case POST_COMMENT,POST_COMMENT_LIKED,POST_COMMENT_MODERATED->postCommentId;
                case POST_REPLY->postReplyId;
                case SHORT_COMMENT,SHORT_COMMENT_LIKED->shortCommentId;
                case SHORT_REPLY->shortReplyId;
                case SOURCE_REPLY->2L;
                case POST_LIKED,POST_MODERATED,GALLERY_POST_PUBLISHED->postId;
                case GALLERY_PROPOSED,GALLERY_APPROVED,GALLERY_REJECTED->proposalId;
                case GALLERY_SUSPENDED,GALLERY_EXPELLED,GALLERY_BAN_LIFTED->banId;
                case SOURCE_IMPORTED->clipId;
                case REPORT_CREATED,REPORT_RESOLVED,REPORT_REJECTED,USER_WARNED,CONTENT_MODERATED->reportId;
                case USER_BANNED,USER_BAN_LIFTED->userBanId;
                default->1L;
            };
            var deliveries=audience.resolve(type,4L,target,1L,"처리 사유");
            assertThat(deliveries).as(type.name()).isNotEmpty();
            for(var delivery:deliveries){
                var n=new NotificationDraft(delivery.recipientId(),delivery.type(),4L,type.title,delivery.body(),delivery.path(),delivery.resourceType(),delivery.resourceId(),delivery.projectId(),delivery.requestId(),type.name(),Instant.now()).entity();
                assertThat(audience.access(delivery.recipientId(),n).available()).as(type+" access").isTrue();
            }
        }
    }
    @Test void collaborationNotificationsNeverIncludeVisitorsOrPendingContributors(){
        var events=new NotificationActivityService(store,audience,u->{},Clock.systemUTC());
        events.record(NotificationType.PROJECT_COMPLETED,1L,projectId);
        assertThat(store.unread(3L)).isEqualTo(1);
        assertThat(store.unread(4L)).isZero();
        assertThat(store.unread(1L)).isZero();
    }
    @Test void preDeploymentPendingRequestsAreBackfilledOnlyOnceEvenAfterRead(){
        var events=new NotificationActivityService(store,audience,u->{},Clock.systemUTC());
        var inbox=new NotificationInboxUseCase(store,audience,TransactionRunner.direct(),u->{},Clock.systemUTC(),events);
        assertThat(inbox.unread(3L)).isEqualTo(1);
        var page=inbox.list(3L,null,"FOLLOW",false,20);
        inbox.readAll(3L,page.throughId());
        assertThat(inbox.unread(3L)).isZero();
        assertThat(inbox.list(3L,null,null,false,20).items()).hasSize(1);
    }
}
