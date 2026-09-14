package glitched.adlips.application.project.port.out;

import glitched.adlips.domain.project.Project;

public interface PublishedShortPort {

    Long publish(Project project, Long exportId, Long mixedAudioFileId);
}
