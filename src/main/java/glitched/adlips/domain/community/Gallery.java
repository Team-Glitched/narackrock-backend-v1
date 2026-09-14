package glitched.adlips.domain.community;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "galleries")
public class Gallery extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    protected Gallery() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
