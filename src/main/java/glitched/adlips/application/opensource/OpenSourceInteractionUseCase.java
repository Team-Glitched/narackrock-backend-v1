package glitched.adlips.application.opensource;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.domain.reaction.ReactionToggle;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import static glitched.adlips.application.opensource.OpenSourceException.Code.*;
import static glitched.adlips.application.opensource.OpenSourceInteractionPort.*;

public class OpenSourceInteractionUseCase {
    private final NotificationEvents notifications;
    public record Created(Long openSourceId, long commentCount, Comment comment) {}
    public record Comments(Long openSourceId, Long parentCommentId, int page, int size,
                           long totalCount, boolean hasNext, List<Comment> comments) {}
    private final OpenSourceInteractionPort port;
    private final TransactionRunner transactions;
    private final UserBanQueryPort bans;
    private final Clock clock;
    public OpenSourceInteractionUseCase(OpenSourceInteractionPort port, TransactionRunner transactions, UserBanQueryPort bans, Clock clock) {
        this(port, transactions, bans, clock, NotificationEvents.none());
    }

    public OpenSourceInteractionUseCase(OpenSourceInteractionPort port, TransactionRunner transactions, UserBanQueryPort bans, Clock clock, NotificationEvents notifications) {
        this.port=port; this.transactions=transactions; this.bans=bans; this.clock=clock;
    
        this.notifications = notifications;
    }
    public ReactionState react(Long userId, Long targetId, Target target, ReactionType requested) {
        writeAllowed(userId); validId(targetId);
        if(target==null || requested==null) throw invalid();
        return transactions.required(() -> {
            Long sourceId=target==Target.SOURCE?targetId:findComment(targetId).openSourceId();
            available(sourceId);
            if(target==Target.COMMENT) findComment(targetId);
            var change=ReactionToggle.of(port.reaction(userId,target,targetId).orElse(null),requested);
            port.setReaction(userId,target,targetId,change.result(),clock.instant());
            if(change.result()==ReactionType.LIKE)notifications.record(target==Target.SOURCE?SOURCE_LIKED:SOURCE_COMMENT_LIKED,userId,targetId);
            return port.reactions(userId,target,targetId);
        });
    }
    public Created create(Long userId, Long sourceId, String content, Long parentId) {
        writeAllowed(userId); validId(sourceId);
        if(content==null || content.strip().isEmpty() || content.strip().codePointCount(0,content.strip().length())>1000) throw invalid();
        String normalized=content.strip();
        return transactions.required(() -> {
            available(sourceId); parent(sourceId,parentId);
            if (parentId != null && port.isCommentHidden(userId, parentId)) throw commentMissing();
            Long id=port.insertComment(sourceId,userId,parentId,normalized,clock.instant());
            notifications.record(parentId==null?SOURCE_COMMENT:SOURCE_REPLY,userId,id);
            return new Created(sourceId,port.visibleCommentCount(sourceId,null,userId,true),port.comment(id,userId));
        });
    }
    public void hideComment(Long userId, Long commentId) {
        validId(userId);
        validId(commentId);
        transactions.required(() -> {
            var comment = findComment(commentId);
            available(comment.openSourceId());
            findComment(commentId);
            port.hideComment(userId, commentId);
            return null;
        });
    }
    public Comments comments(Long userId, Long sourceId, Long parentId, int page, int size) {
        validId(userId); validId(sourceId);
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE) throw invalid();
        // Keep access, total and rows consistent with publication changes and comment writes.
        return transactions.required(() -> {
            available(sourceId); parent(sourceId,parentId);
            var rows=port.comments(sourceId,parentId,userId,page*size,size+1);
            return new Comments(sourceId,parentId,page,size,port.visibleCommentCount(sourceId,parentId,userId,false),rows.size()>size,rows.stream().limit(size).toList());
        });
    }
    private void parent(Long sourceId, Long parentId) {
        if(parentId==null) return;
        validId(parentId);
        var parent=findComment(parentId);
        if(!parent.openSourceId().equals(sourceId)) throw commentMissing();
        if(parent.parentCommentId()!=null) throw invalid();
    }
    private CommentRef findComment(Long id) { return port.findComment(id).orElseThrow(this::commentMissing); }
    private void available(Long id) {
        if(!port.lockAvailableSource(id)) throw new OpenSourceException(OPEN_SOURCE_NOT_FOUND,"존재하지 않거나 접근할 수 없는 오픈소스입니다.");
    }
    private void writeAllowed(Long userId) {
        validId(userId);
        if(bans.isBanned(userId, LocalDateTime.now(clock))) throw new OpenSourceException(OPEN_SOURCE_ACCESS_DENIED,"현재 서비스 이용 정지 상태입니다.");
    }
    private void validId(Long id) { if(id==null || id<=0) throw invalid(); }
    private OpenSourceException invalid() { return new OpenSourceException(INVALID_REQUEST,"반응 또는 댓글 요청값이 올바르지 않습니다."); }
    private OpenSourceException commentMissing() { return new OpenSourceException(OPEN_SOURCE_COMMENT_NOT_FOUND,"존재하지 않거나 접근할 수 없는 댓글입니다."); }
}
