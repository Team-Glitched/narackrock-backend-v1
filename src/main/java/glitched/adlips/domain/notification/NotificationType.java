package glitched.adlips.domain.notification;

public enum NotificationType {
    FOLLOW_CREATED("FOLLOW", "새 팔로워"),
    FOLLOW_REQUESTED("FOLLOW", "팔로우 요청"),
    FOLLOW_ACCEPTED("FOLLOW", "팔로우 요청 수락"),
    FOLLOW_REJECTED("FOLLOW", "팔로우 요청 거절"),
    CONTRIBUTION_CREATED("CONTRIBUTION", "작곡 참여 요청"),
    CONTRIBUTION_ACCEPTED("CONTRIBUTION", "참여 요청 수락"),
    CONTRIBUTION_REJECTED("CONTRIBUTION", "참여 요청 거절"),
    PROJECT_PUBLISHED("CONTRIBUTION", "곡 공개 완료"),
    PROJECT_REFRESHED("CONTRIBUTION", "곡 반영 완료"),
    EXPORT_FAILED("CONTRIBUTION", "곡 공개 실패"),
    PROJECT_COMPLETED("CONTRIBUTION", "곡 완성"),
    PROJECT_DELETED("CONTRIBUTION", "프로젝트 삭제"),
    POST_COMMENT("COMMUNITY", "게시글 댓글"),
    POST_REPLY("COMMUNITY", "댓글 답글"),
    POST_LIKED("COMMUNITY", "게시글 좋아요"),
    POST_COMMENT_LIKED("COMMUNITY", "댓글 좋아요"),
    GALLERY_PROPOSED("COMMUNITY", "커뮤니티 개설 신청"),
    GALLERY_APPROVED("COMMUNITY", "커뮤니티 개설 승인"),
    GALLERY_REJECTED("COMMUNITY", "커뮤니티 개설 반려"),
    POST_MODERATED("COMMUNITY", "게시글 삭제 안내"),
    POST_COMMENT_MODERATED("COMMUNITY", "댓글 삭제 안내"),
    GALLERY_SUSPENDED("COMMUNITY", "커뮤니티 이용 정지"),
    GALLERY_EXPELLED("COMMUNITY", "커뮤니티 추방"),
    GALLERY_BAN_LIFTED("COMMUNITY", "커뮤니티 이용 제한 해제"),
    GALLERY_POST_PUBLISHED("COMMUNITY", "구독 커뮤니티의 새 글"),
    SHORT_COMMENT("SHORT", "음악 댓글"),
    SHORT_REPLY("SHORT", "댓글 답글"),
    SHORT_LIKED("SHORT", "음악 좋아요"),
    SHORT_COMMENT_LIKED("SHORT", "댓글 좋아요"),
    FOLLOWING_SHORT_PUBLISHED("SHORT", "새 음악"),
    SOURCE_COMMENT("OPEN_SOURCE", "오픈소스 댓글"),
    SOURCE_REPLY("OPEN_SOURCE", "댓글 답글"),
    SOURCE_LIKED("OPEN_SOURCE", "오픈소스 좋아요"),
    SOURCE_COMMENT_LIKED("OPEN_SOURCE", "댓글 좋아요"),
    SOURCE_IMPORTED("OPEN_SOURCE", "오픈소스 사용"),
    SOURCE_USED_IN_SHORT("OPEN_SOURCE", "오픈소스 사용곡 공개"),
    FOLLOWING_SOURCE_PUBLISHED("OPEN_SOURCE", "새 오픈소스"),
    REPORT_CREATED("SYSTEM", "새 신고"),
    REPORT_RESOLVED("SYSTEM", "신고 처리 완료"),
    REPORT_REJECTED("SYSTEM", "신고 반려"),
    USER_WARNED("SYSTEM", "운영 경고"),
    USER_BANNED("SYSTEM", "서비스 이용 정지"),
    USER_BAN_LIFTED("SYSTEM", "서비스 이용 정지 해제");
    public final String category;
    public final String title;
    NotificationType(String category, String title) { this.category=category; this.title=title; }
    public boolean reaction() { return name().endsWith("_LIKED"); }
    public boolean followingContent() { return name().startsWith("FOLLOWING_"); }
    public boolean communityPosts() { return this==GALLERY_POST_PUBLISHED; }
    public boolean sourceUsage() { return this==SOURCE_IMPORTED || this==SOURCE_USED_IN_SHORT; }
}
