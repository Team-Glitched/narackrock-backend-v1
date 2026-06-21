package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseUpdatedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "profiles")
public class Profile extends BaseUpdatedEntity {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "primary_instrument")
    private String primaryInstrument;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;
}
