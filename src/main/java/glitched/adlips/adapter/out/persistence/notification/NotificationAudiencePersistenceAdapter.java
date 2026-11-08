package glitched.adlips.adapter.out.persistence.notification;

import glitched.adlips.application.notification.NotificationAudiencePort;
import glitched.adlips.domain.notification.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;
import static glitched.adlips.domain.notification.NotificationType.*;

/** Reads the owners, approved contributors and access state used by the notification workflow. */
@Repository
public class NotificationAudiencePersistenceAdapter implements NotificationAudiencePort {
    private final EntityManager em;
    public NotificationAudiencePersistenceAdapter(EntityManager em){this.em=em;}
    private List<Object[]> rows(String sql,Object... parameters){
        var q=em.createNativeQuery(sql);
        for(int i=0;i<parameters.length;i++)q.setParameter("p"+i,parameters[i]);
        @SuppressWarnings("unchecked") List<Object> result=q.getResultList();
        return result.stream().map(r->r instanceof Object[] a?a:new Object[]{r}).toList();
    }
    private Object[] one(String sql,Object... parameters){return rows(sql,parameters).stream().findFirst().orElse(null);}
    private Long id(Object value){return value==null?null:((Number)value).longValue();}
    private String text(Object value){return value==null?"":value.toString();}
    private boolean exists(String sql,Object... parameters){return !rows(sql,parameters).isEmpty();}
    private String name(Long actor){var r=one("select nickname from profiles where user_id=:p0",actor);return r==null?"사용자":text(r[0]);}
    private List<Long> users(String sql,Object... parameters){return rows(sql,parameters).stream().map(r->id(r[0])).toList();}
    private Delivery item(Long recipient,NotificationType type,String body,String path,String kind,Long resource,Long project,Long request,String key){
        return new Delivery(recipient,type,body,path,kind,resource,project,request,key);
    }
    private List<Delivery> to(List<Long> recipients,NotificationType type,String body,String path,String kind,Long resource,Long project,Long request,String key){
        return recipients.stream().distinct().filter(u->exists("select id from users where id=:p0 and deleted_at is null",u))
            .map(u->item(u,type,body,path,kind,resource,project,request,key)).toList();
    }
    private List<Long> contributors(Long project,boolean pending){
        return users("select owner_id from projects where id=:p0 union select user_id from project_contributions where project_id=:p0 and approval_status "
            +(pending?"in ('APPROVED','PENDING')":"= 'APPROVED'"),project);
    }
    public List<Delivery> resolve(NotificationType type,Long actor,Long target,Long reference,String detail){
        em.flush();
        String who=name(actor),extra=detail==null?"":detail.strip();
        if(type.category.equals("FOLLOW")){
            String body=switch(type){case FOLLOW_CREATED->who+"님이 회원님을 팔로우했습니다.";case FOLLOW_REQUESTED->who+"님이 팔로우를 요청했습니다.";
                case FOLLOW_ACCEPTED->who+"님이 팔로우 요청을 수락했습니다.";default->who+"님이 팔로우 요청을 거절했습니다.";};
            return to(List.of(target),type,body,"/users/"+actor,"USER",actor,null,type==FOLLOW_REQUESTED?reference:null,null);
        }
        if(type==CONTRIBUTION_CREATED||type==CONTRIBUTION_ACCEPTED||type==CONTRIBUTION_REJECTED){
            var c=one("select c.user_id,p.owner_id,p.id,p.title,c.review_comment from project_contributions c join projects p on p.id=c.project_id where c.id=:p0",target);
            if(c==null)return List.of();
            Long project=id(c[2]);String suffix=type==CONTRIBUTION_CREATED?"참여를 요청했습니다.":type==CONTRIBUTION_ACCEPTED?"참여 요청을 수락했습니다.":"참여 요청을 거절했습니다.";
            return to(List.of(id(c[type==CONTRIBUTION_CREATED?1:0])),type,who+"님이 ‘"+text(c[3])+"’ "+suffix+(text(c[4]).isBlank()?"":"\n검토 의견: "+text(c[4])),
                "/submissions/"+target+"?projectId="+project,"PROJECT",project,project,target,null);
        }
        if(type==PROJECT_PUBLISHED||type==PROJECT_REFRESHED||type==FOLLOWING_SHORT_PUBLISHED||type==SOURCE_USED_IN_SHORT){
            var s=one("select s.user_id,s.project_id,s.title from shorts s where s.id=:p0 and s.deleted_at is null",target);
            if(s==null)return List.of();Long project=id(s[1]);
            if(type==FOLLOWING_SHORT_PUBLISHED){
                // In-progress updates never re-announce the same public song.
                return to(users("select follower_id from follows where following_id=:p0",id(s[0])),type,"‘"+text(s[2])+"’ 곡이 공개되었습니다.","/abyss/"+target,"SHORT",target,project,null,"following-short:"+target);
            }
            if(type==SOURCE_USED_IN_SHORT){
                if(project==null)return List.of();
                return to(users("select distinct os.owner_id from project_clips c join project_tracks t on t.id=c.track_id join open_sources os on os.id=c.origin_open_source_id where c.project_id=:p0 and c.is_deleted=false and t.is_deleted=false and c.approval_status='APPROVED' and t.approval_status='APPROVED'",project),type,
                    "회원님의 오픈소스를 사용한 ‘"+text(s[2])+"’ 곡이 공개되었습니다.","/abyss/"+target,"SHORT",target,project,null,"source-used-short:"+target);
            }
            boolean refreshed=exists("select id from service_notifications where resource_type='SHORT' and resource_id=:p0 and type='PROJECT_PUBLISHED'",target);
            NotificationType actual=refreshed?PROJECT_REFRESHED:PROJECT_PUBLISHED;
            return to(refreshed&&project!=null?contributors(project,false):List.of(id(s[0])),actual,"‘"+text(s[2])+"’ 곡의 "+(refreshed?"승인된 변경 사항이 반영되었습니다.":"공개가 완료되었습니다."),
                "/abyss/"+target,"SHORT",target,project,null,"export:"+reference);
        }
        if(type==EXPORT_FAILED||type==PROJECT_COMPLETED||type==PROJECT_DELETED){
            var p=one("select owner_id,title from projects where id=:p0",target);if(p==null)return List.of();
            String body="‘"+text(p[1])+"’ "+(type==EXPORT_FAILED?"공개 처리에 실패했습니다. 작업 화면에서 상태를 확인해 주세요.":type==PROJECT_COMPLETED?"곡이 완성되었습니다.":"프로젝트가 삭제되어 참여 요청과 작업을 진행할 수 없습니다.");
            return to(type==EXPORT_FAILED?List.of(id(p[0])):contributors(target,type==PROJECT_DELETED),type,body,
                type==PROJECT_DELETED?null:"/studio/"+target,type==PROJECT_DELETED?"NOTICE":"PROJECT",target,target,null,null);
        }
        if(type==POST_COMMENT||type==POST_REPLY||type==POST_COMMENT_LIKED||type==SHORT_COMMENT||type==SHORT_REPLY||type==SHORT_COMMENT_LIKED||type==SOURCE_COMMENT||type==SOURCE_REPLY||type==SOURCE_COMMENT_LIKED){
            boolean post=type.category.equals("COMMUNITY"),source=type.category.equals("OPEN_SOURCE");
            String table=post?"post_comments":source?"open_source_comments":"shorts_comments";
            String parent=post?"posts":source?"open_sources":"shorts";
            String fk=post?"post_id":source?"open_source_id":"shorts_id";
            String owner=source?"owner_id":"user_id";
            var c=one("select c.user_id,c.parent_comment_id,p."+owner+",p.id,p.title from "+table+" c join "+parent+" p on p.id=c."+fk+" where c.id=:p0",target);
            if(c==null)return List.of();
            Long recipient=id(c[2]);
            if(type.reaction())recipient=id(c[0]);
            else if(c[1]!=null){var r=one("select user_id from "+table+" where id=:p0",id(c[1]));if(r==null)return List.of();recipient=id(r[0]);}
            String path=post?"/posts/"+c[3]:source?"/sources/"+c[3]+"/comments":"/abyss/"+c[3];
            path+="?commentId="+target+(c[1]==null?"":"&parentCommentId="+c[1]);
            return to(List.of(recipient),type,who+"님이 ‘"+text(c[4])+"’에서 "+(type.reaction()?"회원님의 댓글을 좋아합니다.":c[1]!=null?"회원님의 댓글에 답글을 남겼습니다.":"댓글을 남겼습니다."),path,
                post?"POST":source?"SOURCE":"SHORT",id(c[3]),null,null,null);
        }
        if(type==POST_LIKED||type==SHORT_LIKED||type==SOURCE_LIKED){
            String table=type==POST_LIKED?"posts":type==SHORT_LIKED?"shorts":"open_sources";
            var p=one("select "+(type==SOURCE_LIKED?"owner_id":"user_id")+",title from "+table+" where id=:p0",target);if(p==null)return List.of();
            return to(List.of(id(p[0])),type,who+"님이 ‘"+text(p[1])+"’을 좋아합니다.",(type==POST_LIKED?"/posts/":type==SHORT_LIKED?"/abyss/":"/sources/")+target,
                type==POST_LIKED?"POST":type==SHORT_LIKED?"SHORT":"SOURCE",target,null,null,null);
        }
        if(type==GALLERY_POST_PUBLISHED){
            var p=one("select gallery_id,title from posts where id=:p0",target);if(p==null)return List.of();
            return to(users("select user_id from gallery_subs where gallery_id=:p0",id(p[0])),type,"구독한 커뮤니티에 ‘"+text(p[1])+"’ 게시글이 올라왔습니다.","/posts/"+target,"POST",target,null,null,null);
        }
        if(type==GALLERY_PROPOSED||type==GALLERY_APPROVED||type==GALLERY_REJECTED){
            var p=one("select user_id,name,rejection_reason from gallery_proposals where id=:p0",target);if(p==null)return List.of();
            return to(type==GALLERY_PROPOSED?users("select id from users where role='ADMIN' and deleted_at is null"):List.of(id(p[0])),type,
                "‘"+text(p[1])+"’ 커뮤니티 개설 "+(type==GALLERY_PROPOSED?"신청이 도착했습니다.":type==GALLERY_APPROVED?"신청이 승인되었습니다.":"신청이 반려되었습니다.\n사유: "+text(p[2])),
                type==GALLERY_PROPOSED?"/admin":type==GALLERY_APPROVED&&reference!=null?"/communities/"+reference:"/communities/new",type==GALLERY_PROPOSED?"ADMIN":"NOTICE",target,null,null,null);
        }
        if(type==POST_MODERATED||type==POST_COMMENT_MODERATED){
            String table=type==POST_MODERATED?"posts":"post_comments";var p=one("select user_id from "+table+" where id=:p0",target);if(p==null)return List.of();
            return to(List.of(id(p[0])),type,"운영 정책에 따라 회원님의 "+(type==POST_MODERATED?"게시글":"댓글")+"이 삭제되었습니다.\n사유: "+extra,null,"NOTICE",target,null,null,null);
        }
        if(type==GALLERY_SUSPENDED||type==GALLERY_EXPELLED||type==GALLERY_BAN_LIFTED){
            var p=one("select b.user_id,b.gallery_id,g.name,b.reason,b.banned_until from gallery_bans b join galleries g on g.id=b.gallery_id where b.id=:p0",target);if(p==null)return List.of();
            return to(List.of(id(p[0])),type,"‘"+text(p[2])+"’ 커뮤니티에서 "+(type==GALLERY_BAN_LIFTED?"이용 제한이 해제되었습니다.":type==GALLERY_EXPELLED?"추방되었습니다.":"이용이 정지되었습니다.")
                +(type==GALLERY_BAN_LIFTED?"":"\n사유: "+text(p[3])+"\n기간: "+(p[4]==null?"기한 없음":text(p[4]))),null,"NOTICE",target,null,null,null);
        }
        if(type==FOLLOWING_SOURCE_PUBLISHED){
            var p=one("select owner_id,title from open_sources where id=:p0 and is_public=true and deleted_at is null",target);if(p==null)return List.of();
            return to(users("select follower_id from follows where following_id=:p0",id(p[0])),type,"팔로우한 사용자가 ‘"+text(p[1])+"’ 오픈소스를 공개했습니다.","/sources/"+target,"SOURCE",target,null,null,null);
        }
        if(type==SOURCE_IMPORTED){
            var p=one("select os.owner_id,os.id,os.title from project_clips c join open_sources os on os.id=c.origin_open_source_id where c.id=:p0",target);if(p==null)return List.of();
            // Never expose the importing user's private project title or link.
            return to(List.of(id(p[0])),type,who+"님이 ‘"+text(p[2])+"’ 오픈소스를 작곡에 사용했습니다.","/sources/"+p[1],"SOURCE",id(p[1]),null,null,"source-import:"+actor+":"+p[1]+":"+reference);
        }
        if(type==REPORT_CREATED||type==REPORT_RESOLVED||type==REPORT_REJECTED||type==USER_WARNED||type==CONTENT_MODERATED){
            var p=one("select reporter_id,target_type,target_id from reports where id=:p0",target);if(p==null)return List.of();
            List<Long> recipients;
            if(type==REPORT_CREATED)recipients=users("select id from users where role='ADMIN' and deleted_at is null");
            else if(type==USER_WARNED||type==CONTENT_MODERATED){
                String table=switch(text(p[1])){case "POST"->"posts";case "POST_COMMENT"->"post_comments";case "SHORT"->"shorts";case "SHORT_COMMENT"->"shorts_comments";default->null;};
                recipients=table==null?List.of():users("select user_id from "+table+" where id=:p0",id(p[2]));
            }else recipients=List.of(id(p[0]));
            String body=type==REPORT_CREATED?"새 신고가 접수되었습니다.":type==USER_WARNED?"운영 정책 위반으로 경고가 부여되었습니다.":type==CONTENT_MODERATED?"운영 정책에 따라 회원님의 콘텐츠가 삭제되었습니다.":type==REPORT_RESOLVED?"회원님의 신고가 검토되어 처리되었습니다.":"회원님의 신고가 반려되었습니다.";
            return to(recipients,type,body+(extra.isEmpty()?"":"\n안내: "+extra),type==REPORT_CREATED?"/admin":null,type==REPORT_CREATED?"ADMIN":"NOTICE",target,null,null,null);
        }
        if(type==USER_BANNED||type==USER_BAN_LIFTED){
            var p=one("select user_id,reason,banned_until from user_bans where id=:p0",target);if(p==null)return List.of();
            return to(List.of(id(p[0])),type,type==USER_BAN_LIFTED?"서비스 이용 정지가 해제되었습니다.":"서비스 이용이 정지되었습니다.\n사유: "+text(p[1])+"\n기간: "+(p[2]==null?"기한 없음":text(p[2])),null,"NOTICE",target,null,null,null);
        }
        return List.of();
    }
    public Access access(Long user,ServiceNotification n){
        String kind=n.getResourceType();Long id=n.getResourceId();String status=null;
        if(n.getType()==FOLLOW_REQUESTED){status=exists("select id from follow_requests where id=:p0 and target_id=:p1",n.getRequestId(),user)?"PENDING":"CLOSED";}
        if(n.getRequestId()!=null&&n.getType().category.equals("CONTRIBUTION")){
            var c=one("select approval_status from project_contributions where id=:p0",n.getRequestId());status=c==null?"CLOSED":text(c[0]);
        }
        boolean available=switch(kind==null?"NOTICE":kind){
            case "POST"->exists("select id from posts where id=:p0 and deleted_at is null",id);
            case "SHORT"->exists("select s.id from shorts s left join projects p on p.id=s.project_id where s.id=:p0 and s.deleted_at is null and (s.project_id is null or (p.deleted_at is null and p.is_public=true))",id);
            case "SOURCE"->exists("select id from open_sources where id=:p0 and deleted_at is null and is_public=true",id);
            case "USER"->exists("select id from users where id=:p0 and deleted_at is null",id);
            case "PROJECT"->exists("select p.id from projects p where p.id=:p0 and p.deleted_at is null and (p.owner_id=:p1 or exists(select c.id from project_contributions c where c.project_id=p.id and c.user_id=:p1))",id,user);
            case "ADMIN"->exists("select id from users where id=:p0 and role='ADMIN' and deleted_at is null",user);
            default->true;
        };
        return new Access(available,status);
    }
}
