package glitched.adlips.adapter.out.persistence.opensource;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="open_source_comments", indexes=@Index(name="idx_os_comments_source_parent_time",columnList="open_source_id,parent_comment_id,created_at,id"))
public class OpenSourceCommentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="open_source_id",nullable=false) Long openSourceId;
    @Column(name="user_id",nullable=false) Long userId;
    @Column(name="parent_comment_id") Long parentCommentId;
    @Column(nullable=false,columnDefinition="TEXT") String content;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="deleted_at") Instant deletedAt;
    protected OpenSourceCommentEntity() {}
    OpenSourceCommentEntity(Long sourceId,Long userId,Long parentId,String content,Instant now) {
        this.openSourceId=sourceId;this.userId=userId;this.parentCommentId=parentId;this.content=content;this.createdAt=now;
    }
}
