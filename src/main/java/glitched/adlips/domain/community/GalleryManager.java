package glitched.adlips.domain.community;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(name = "gallery_managers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"gallery_id", "user_id"}),
        indexes = @Index(name = "idx_gallery_managers_user", columnList = "user_id"))
public class GalleryManager extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private GalleryRole role;

    protected GalleryManager() {
    }

    private GalleryManager(Gallery gallery, User user, GalleryRole role) {
        this.gallery = Objects.requireNonNull(gallery, "gallery must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public static GalleryManager assign(Gallery gallery, User user, GalleryRole role) {
        return new GalleryManager(gallery, user, role);
    }

    public Long getId() {
        return id;
    }

    public Long getGalleryId() {
        return gallery.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    public GalleryRole getRole() {
        return role;
    }
}
