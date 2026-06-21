package glitched.adlips.domain.project;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "cover_image")
    private String coverImage;
    @Column(name = "genre") private String genre;
    @Column(name = "bpm") private Integer bpm;
    @Column(name = "song_key")
    private String songKey;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.DRAFT;
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
