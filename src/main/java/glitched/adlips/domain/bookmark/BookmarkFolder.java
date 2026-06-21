package glitched.adlips.domain.bookmark;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "bookmark_folders", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
public class BookmarkFolder extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "name", nullable = false)
    private String name;
}
