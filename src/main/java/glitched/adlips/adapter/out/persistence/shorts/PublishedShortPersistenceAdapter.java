package glitched.adlips.adapter.out.persistence.shorts;

import glitched.adlips.adapter.out.persistence.project.ProjectMemberJpaRepository;
import glitched.adlips.application.project.port.out.PublishedShortPort;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.shorts.ShortParticipant;
import glitched.adlips.domain.shorts.ShortStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
        ShortForm shortForm = shortForms.findByProjectIdAndDeletedAtIsNull(project.getId()).stream()
                .filter(shortFormItem -> shortFormItem.getStatus() == ShortStatus.IN_PROGRESS)
                .max(Comparator.comparing(ShortForm::getId))
                .orElse(null);
        if (shortForm == null) {
            shortForm = shortForms.save(ShortForm.fromExport(project, exportId, mixedAudioFileId));
        } else {
            shortForm.refreshFromExport(exportId, mixedAudioFileId);
            shortForm = shortForms.save(shortForm);
        }
        Set<Long> existingUserIds = participants.findByShortsId(shortForm.getId()).stream()
                .map(participant -> participant.getUser().getId())
                .collect(Collectors.toSet());
        ShortForm target = shortForm;
        List<ShortParticipant> shortParticipants = projectMembers
                .findByProjectIdOrderByIdAsc(project.getId())
                .stream()
                .filter(member -> !existingUserIds.contains(member.getUser().getId()))
                .map(member -> new ShortParticipant(
                        target, member.getUser(), member.getRole().name()))
                .toList();
        participants.saveAll(shortParticipants);
        return shortForm.getId();
    }

    @Override
    public void completeProject(Long projectId) {
        shortForms.findByProjectIdAndDeletedAtIsNull(projectId).forEach(ShortForm::complete);
    }
}
