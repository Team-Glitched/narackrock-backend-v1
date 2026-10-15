package glitched.adlips.domain.community;

/**
 * 갤러리 관리 권한 규칙. 두 행위자는 같은 갤러리 기준으로 조립된 것이어야 한다.
 */
public final class GalleryModerationPolicy {

    private GalleryModerationPolicy() {
    }

    public static boolean canManage(GalleryActor actor) {
        return !actor.globallyBanned() && (actor.isAdmin() || actor.isManager());
    }

    /** 매니저는 일반 이용자만, 어드민은 본인과 다른 어드민을 제외한 누구나 제재할 수 있다. */
    public static boolean canSanction(GalleryActor actor, GalleryActor target) {
        if (!canManage(actor) || actor.userId().equals(target.userId()) || target.isAdmin()) {
            return false;
        }
        return actor.isAdmin() || !target.isManager();
    }

    /** 매니저는 어드민이 작성한 글·댓글을 삭제할 수 없다. */
    public static boolean canDeleteContentOf(GalleryActor actor, GalleryActor author) {
        return canManage(actor) && (actor.isAdmin() || !author.isAdmin());
    }
}
