package glitched.adlips.domain.community;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "galleries")
public class Gallery extends BaseCreatedEntity {
    static final int NAME_MIN_LENGTH = 2;
    static final int NAME_MAX_LENGTH = 30;
    static final int DESCRIPTION_MAX_LENGTH = 500;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    protected Gallery() {
    }

    private Gallery(String name, String description) {
        this.name = requireName(name);
        this.description = requireDescription(description);
    }

    public static Gallery create(String name, String description) {
        return new Gallery(name, description);
    }

    public void changeDescription(String description) {
        this.description = requireDescription(description);
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

    static String requireName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("갤러리 이름은 2자 이상 30자 이하여야 합니다.");
        }
        return trimmed;
    }

    static String requireDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty() || trimmed.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("갤러리 소개는 1자 이상 500자 이하여야 합니다.");
        }
        return trimmed;
    }
}
