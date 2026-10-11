package glitched.adlips.domain.community;

import glitched.adlips.domain.bookmark.BookmarkFolder;
import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

// ERD에 게시글 북마크 테이블이 없어 shorts_bookmarks와 같은 구조로 둔다.
@Entity
@Table(
        name = "post_bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_bookmarks_user_post",
                columnNames = {"user_id", "post_id"}
        ),
        indexes = @Index(
                name = "idx_post_bookmarks_user_folder",
                columnList = "user_id, folder_id"
        )
)
public class PostBookmark extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "folder_id") private BookmarkFolder folder;
}
