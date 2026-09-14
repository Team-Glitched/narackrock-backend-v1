package glitched.adlips.application.user.relation.port.out;

import java.util.List;

public interface CollaborationUserQueryPort {
    List<Long> findCollaboratorIds(Long userId);
}
