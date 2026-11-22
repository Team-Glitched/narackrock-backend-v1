package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.OpenSourceInteractionPort;
import glitched.adlips.application.opensource.OpenSourceQueryPort;
import glitched.adlips.domain.reaction.ReactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import static glitched.adlips.adapter.out.persistence.opensource.OpenSourceInteractionSql.*;

@Repository
public class OpenSourceInteractionPersistenceAdapter implements OpenSourceInteractionPort {
    private final EntityManager em;
    private final NamedParameterJdbcTemplate jdbc;
    private static final String COMMENT_SELECT="""
            SELECT c.id,c.open_source_id,c.parent_comment_id,c.user_id,c.content,c.created_at,
                   COALESCE(p.nickname,'사용자') AS nickname, avatar.file_url AS profile_image_url,
            """+count("COMMENT","c.id","LIKE")+" AS like_count, "+count("COMMENT","c.id","DISLIKE")+" AS dislike_count, "
            +reacted("COMMENT","c.id","LIKE")+" AS is_liked, "+reacted("COMMENT","c.id","DISLIKE")+" AS is_disliked, "
            +"(SELECT COUNT(*) FROM open_source_comments child WHERE child.parent_comment_id=c.id AND "+visibleComment("child")+notHidden("child")+") AS reply_count "
            +"FROM open_source_comments c LEFT JOIN profiles p ON p.user_id=c.user_id LEFT JOIN media_files avatar ON avatar.id=p.profile_image_file_id "
            +"AND avatar.owner_id=c.user_id AND avatar.status='READY' AND avatar.file_type='IMAGE' WHERE "+visibleComment("c");
    public OpenSourceInteractionPersistenceAdapter(EntityManager em,NamedParameterJdbcTemplate jdbc) { this.em=em;this.jdbc=jdbc; }
    @Override public boolean lockAvailableSource(Long id) {
        return !em.createQuery("""
                SELECT s FROM OpenSourceEntity s WHERE s.id=:id AND s.isPublic=true AND s.deletedAt IS NULL
                AND s.title IS NOT NULL AND s.mediaFileId IS NOT NULL AND s.createdAt IS NOT NULL AND s.updatedAt IS NOT NULL
                AND EXISTS (SELECT u.id FROM User u WHERE u.id=s.ownerId AND u.deletedAt IS NULL)
                """,OpenSourceEntity.class).setParameter("id",id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().isEmpty();
    }
    @Override
    public boolean recordView(Long sourceId, Long viewerId, Instant now, Instant cutoff) {
        // The publication is locked before checking or inserting a view.
        var view = em.createQuery("""
                SELECT v FROM OpenSourceViewEntity v
                WHERE v.openSourceId = :sourceId AND v.viewerId = :viewerId
                """, OpenSourceViewEntity.class)
                .setParameter("sourceId", sourceId)
                .setParameter("viewerId", viewerId)
                .getSingleResultOrNull();
        if (view == null) {
            em.persist(new OpenSourceViewEntity(sourceId, viewerId, now));
            return true;
        }
        if (view.lastViewedAt.isAfter(cutoff)) return false;
        view.lastViewedAt = now;
        view.viewCount++;
        return true;
    }

    private Optional<OpenSourceReactionEntity> entity(Long user,Target target,Long id) {
        return em.createQuery("SELECT r FROM OpenSourceReactionEntity r WHERE r.userId=:user AND r.target=:target AND r.targetId=:id",OpenSourceReactionEntity.class)
                .setParameter("user",user).setParameter("target",target).setParameter("id",id).getResultStream().findFirst();
    }
    @Override public Optional<ReactionType> reaction(Long user,Target target,Long id) { return entity(user,target,id).map(r->r.reaction); }
    @Override public void setReaction(Long user,Target target,Long id,ReactionType reaction,Instant now) {
        var existing=entity(user,target,id);
        if(reaction==null) existing.ifPresent(em::remove);
        else if(existing.isPresent()) existing.get().reaction=reaction;
        else em.persist(new OpenSourceReactionEntity(user,target,id,reaction,now));
        em.flush();
    }
    @Override public ReactionState reactions(Long user,Target target,Long id) {
        String t=target.name();
        return jdbc.queryForObject("SELECT "+count(t,":id","LIKE")+" AS likes,"+count(t,":id","DISLIKE")+" AS dislikes,"
                +reacted(t,":id","LIKE")+" AS liked,"+reacted(t,":id","DISLIKE")+" AS disliked",
                Map.of("id",id,"viewer",user),(rs,row)->new ReactionState(rs.getBoolean("liked"),rs.getBoolean("disliked"),rs.getLong("likes"),rs.getLong("dislikes")));
    }
    @Override public Optional<CommentRef> findComment(Long id) {
        return jdbc.query("SELECT c.id,c.open_source_id,c.parent_comment_id FROM open_source_comments c WHERE c.id=:id AND "+visibleComment("c"),Map.of("id",id),
                (rs,row)->new CommentRef(rs.getLong("id"),rs.getLong("open_source_id"),rs.getObject("parent_comment_id",Long.class))).stream().findFirst();
    }
    @Override public Optional<OwnedCommentRef> findOwnedComment(Long id) {
        return jdbc.query("SELECT c.id,c.open_source_id,c.parent_comment_id,c.user_id FROM open_source_comments c WHERE c.id=:id AND "+visibleComment("c"),Map.of("id",id),
                (rs,row)->new OwnedCommentRef(rs.getLong("id"),rs.getLong("open_source_id"),rs.getObject("parent_comment_id",Long.class),rs.getLong("user_id"))).stream().findFirst();
    }
    @Override public Long insertComment(Long source,Long user,Long parent,String content,Instant now) {
        var comment=new OpenSourceCommentEntity(source,user,parent,content,now);em.persist(comment);em.flush();return comment.id;
    }
    @Override public void updateComment(Long id, String content) {
        jdbc.update("UPDATE open_source_comments SET content=:content WHERE id=:id AND deleted_at IS NULL",
                Map.of("id", id, "content", content));
    }
    @Override public void deleteComment(Long id, Instant now) {
        jdbc.update("UPDATE open_source_comments SET deleted_at=:now WHERE (id=:id OR parent_comment_id=:id) AND deleted_at IS NULL",
                Map.of("id", id, "now", java.sql.Timestamp.from(now)));
    }
    @Override public Comment comment(Long id,Long viewer) {
        return jdbc.queryForObject(COMMENT_SELECT+" AND c.id=:id",Map.of("id",id,"viewer",viewer),(rs,row)->mapComment(rs));
    }
    @Override public boolean isCommentHidden(Long userId, Long commentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM open_source_comment_hides WHERE user_id=:viewer AND comment_id=:comment)
                """, Map.of("viewer", userId, "comment", commentId), Boolean.class));
    }
    @Override public void hideComment(Long userId, Long commentId) {
        jdbc.update("""
                INSERT INTO open_source_comment_hides (user_id, comment_id)
                SELECT :viewer, :comment WHERE NOT EXISTS (
                    SELECT 1 FROM open_source_comment_hides WHERE user_id=:viewer AND comment_id=:comment
                )
                """, Map.of("viewer", userId, "comment", commentId));
    }
    @Override public long visibleCommentCount(Long source, Long parent, Long viewer, boolean all) {
        String parentClause = all ? "" : parent == null ? " AND c.parent_comment_id IS NULL" : " AND c.parent_comment_id=:parent";
        return jdbc.queryForObject("SELECT COUNT(*) FROM open_source_comments c WHERE c.open_source_id=:source AND "
                        + visibleComment("c") + notHidden("c") + parentClause,
                new MapSqlParameterSource("source", source).addValue("parent", parent).addValue("viewer", viewer),
                Long.class);
    }
    @Override public List<Comment> comments(Long source,Long parent,Long viewer,int offset,int limit) {
        var params=new MapSqlParameterSource("source",source).addValue("parent",parent).addValue("viewer",viewer).addValue("offset",offset).addValue("limit",limit);
        return jdbc.query(COMMENT_SELECT+notHidden("c")+" AND c.open_source_id=:source AND "+(parent==null?"c.parent_comment_id IS NULL":"c.parent_comment_id=:parent")
                +" ORDER BY c.created_at ASC,c.id ASC LIMIT :limit OFFSET :offset",params,(rs,row)->mapComment(rs));
    }
    private Comment mapComment(ResultSet rs) throws SQLException {
        return new Comment(rs.getLong("id"),rs.getObject("parent_comment_id",Long.class),
                new OpenSourceQueryPort.Writer(rs.getLong("user_id"),rs.getString("nickname"),rs.getString("profile_image_url")),rs.getString("content"),
                rs.getLong("reply_count"),rs.getBoolean("is_liked"),rs.getBoolean("is_disliked"),rs.getLong("like_count"),rs.getLong("dislike_count"),rs.getTimestamp("created_at").toInstant());
    }
}
