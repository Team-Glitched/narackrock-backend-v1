package glitched.adlips.domain.shorts;

import glitched.adlips.domain.bookmark.BookmarkFolder;
import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "shorts_bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "shorts_id"}),
        indexes = @Index(
                name = "idx_shorts_bookmarks_user_folder",
                columnList = "user_id, folder_id"
        )
)
public class ShortBookmark extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shorts_id", nullable = false) private ShortForm shorts;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "folder_id") private BookmarkFolder folder;
}
