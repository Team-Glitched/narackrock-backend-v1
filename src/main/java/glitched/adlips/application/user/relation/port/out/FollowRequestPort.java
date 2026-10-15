package glitched.adlips.application.user.relation.port.out;

import java.util.List;

public interface FollowRequestPort {
    record Request(Long id, Long requesterId, Long targetId) {}

    boolean exists(Long requesterId, Long targetId);
    Long create(Long requesterId, Long targetId);
    Request find(Long requestId);
    List<Request> inbox(Long targetId);
    List<Request> outgoing(Long requesterId);
    boolean delete(Long requestId);
    boolean cancel(Long requesterId, Long targetId);
}
