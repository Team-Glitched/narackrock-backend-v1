package glitched.adlips.adapter.out.persistence.opensource;

final class OpenSourceInteractionSql {
    private OpenSourceInteractionSql() {}
    static String visibleComment(String alias) {
        return alias+".deleted_at IS NULL AND EXISTS (SELECT 1 FROM users cu WHERE cu.id="+alias+".user_id AND cu.deleted_at IS NULL)"
                +" AND ("+alias+".parent_comment_id IS NULL OR EXISTS (SELECT 1 FROM open_source_comments parent JOIN users pu ON pu.id=parent.user_id"
                +" WHERE parent.id="+alias+".parent_comment_id AND parent.open_source_id="+alias+".open_source_id AND parent.deleted_at IS NULL AND pu.deleted_at IS NULL))";
    }
    static String count(String target,String id,String reaction) {
        return "(SELECT COUNT(*) FROM open_source_reactions r WHERE r.target_type='"+target+"' AND r.target_id="+id+" AND r.reaction_type='"+reaction+"')";
    }
    static String reacted(String target,String id,String reaction) {
        return "EXISTS (SELECT 1 FROM open_source_reactions r WHERE r.target_type='"+target+"' AND r.target_id="+id+" AND r.reaction_type='"+reaction+"' AND r.user_id=:viewer)";
    }
}
