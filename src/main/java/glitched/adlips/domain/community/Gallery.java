package glitched.adlips.domain.community;

import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "galleries")
public class Gallery extends BaseCreatedEntity {
    static final int NAME_MIN_LENGTH = ContentLimits.COMMUNITY_NAME_MIN_LENGTH;
    static final int NAME_MAX_LENGTH = ContentLimits.COMMUNITY_NAME_MAX_LENGTH;
    static final int DESCRIPTION_MAX_LENGTH = ContentLimits.COMMUNITY_DESCRIPTION_MAX_LENGTH;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image_file_id")
    private Long imageFileId;

    @Column(name = "banner_image_file_id")
    private Long bannerImageFileId;

    public Long getImageFileId() { return imageFileId; }
    public Long getBannerImageFileId() { return bannerImageFileId; }

    public void changeImages(Long imageFileId, Long bannerImageFileId) {
        if ((imageFileId != null && imageFileId < 1) || (bannerImageFileId != null && bannerImageFileId < 1))
            throw new IllegalArgumentException("유효한 이미지 파일을 선택해 주세요.");
        this.imageFileId = imageFileId;
        this.bannerImageFileId = bannerImageFileId;
    }

    protected Gallery() {
    }

    private Gallery(String name, String description) {
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
    }

    public static Gallery create(String name, String description) {
        return new Gallery(name, description);
    }

    public void changeDescription(String description) {
        this.description = normalizeDescription(description);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        int length = ContentLimits.codePointLength(trimmed);
        if (length < NAME_MIN_LENGTH || length > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "갤러리 이름은 " + NAME_MIN_LENGTH + "자 이상 " + NAME_MAX_LENGTH + "자 이하여야 합니다.");
        }
        return trimmed;
    }

    public static String normalizeDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        int length = ContentLimits.codePointLength(trimmed);
        if (trimmed.isEmpty() || length > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("갤러리 소개는 1자 이상 " + DESCRIPTION_MAX_LENGTH + "자 이하여야 합니다.");
        }
        return trimmed;
    }
}
