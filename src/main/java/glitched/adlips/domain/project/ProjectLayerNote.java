package glitched.adlips.domain.project;

import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "project_layer_notes")
public class ProjectLayerNote extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "layer_id", nullable = false)
    private ProjectLayer layer;
    @Column(name = "pitch", nullable = false)
    private int pitch;
    @Column(name = "start_time_ms", nullable = false)
    private int startTimeMs;
    @Column(name = "duration_ms", nullable = false)
    private int durationMs;
    @Column(name = "velocity", nullable = false)
    private int velocity = 100;
    @Column(name = "effect_type")
    private String effectType;
    @Column(name = "effect_params", columnDefinition = "TEXT")
    private String effectParams;
}
