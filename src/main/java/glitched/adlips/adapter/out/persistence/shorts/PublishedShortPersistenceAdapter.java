package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortParticipant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PublishedShortPersistenceAdapter implements PublishedShortPort {

    private final ShortFormJpaRepository shortForms;
    private final ShortParticipantJpaRepository participants;
    private final ProjectMemberJpaRepository projectMembers;

    public PublishedShortPersistenceAdapter(
            ShortFormJpaRepository shortForms,
            ShortParticipantJpaRepository participants,
            ProjectMemberJpaRepository projectMembers
    ) {
        this.shortForms = shortForms;
        this.participants = participants;
        this.projectMembers = projectMembers;
    }

    @Override
    public Long publish(Project project, Long exportId, Long mixedAudioFileId) {
        ShortForm shortForm = shortForms.save(
                ShortForm.completedFromExport(project, exportId, mixedAudioFileId));
        List<ShortParticipant> shortParticipants = projectMembers
                .findByProjectIdOrderByIdAsc(project.getId())
                .stream()
                .map(member -> new ShortParticipant(
                        shortForm, member.getUser(), member.getRole().name()))
                .toList();
        participants.saveAll(shortParticipants);
        return shortForm.getId();
    }
}
