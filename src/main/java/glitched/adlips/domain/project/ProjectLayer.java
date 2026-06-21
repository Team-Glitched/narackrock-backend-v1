package glitched.adlips.domain.project;

import glitched.adlips.domain.user.User;
import glitched.adlips.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "project_layers")
public class ProjectLayer extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participant_id")
    private ProjectParticipant participant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "name") private String name;
    @Enumerated(EnumType.STRING) @Column(name = "layer_type", nullable = false)
    private LayerType layerType;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false)
    private LayerSourceType sourceType;
    @Column(name = "instrument") private String instrument;
    @Column(name = "file_url")
    private String fileUrl;
    @Column(name = "waveform_url")
    private String waveformUrl;
    @Column(name = "volume", nullable = false)
    private int volume = 100;
    @Column(name = "pan", nullable = false)
    private int pan = 0;
    @Column(name = "start_time_ms", nullable = false)
    private int startTimeMs = 0;
    @Column(name = "duration_ms")
    private Integer durationMs;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;
    @Column(name = "is_solo", nullable = false)
    private boolean isSolo = false;
    @Enumerated(EnumType.STRING) @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
