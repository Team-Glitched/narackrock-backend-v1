package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.relation.port.out.FollowRequestPort;
import glitched.adlips.domain.user.FollowRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataFollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    boolean existsByRequesterIdAndTargetId(Long requesterId, Long targetId);
    List<FollowRequest> findAllByTargetIdOrderByIdDesc(Long targetId);
    List<FollowRequest> findAllByRequesterIdOrderByIdDesc(Long requesterId);
    long deleteByRequesterIdAndTargetId(Long requesterId, Long targetId);
}

@Repository
public class FollowRequestPersistenceAdapter implements FollowRequestPort {
    private final SpringDataFollowRequestRepository repository;
    public FollowRequestPersistenceAdapter(SpringDataFollowRequestRepository repository) { this.repository = repository; }
    public boolean exists(Long requesterId, Long targetId) { return repository.existsByRequesterIdAndTargetId(requesterId, targetId); }
    public Long create(Long requesterId, Long targetId) { return repository.save(new FollowRequest(requesterId, targetId)).getId(); }
    public Request find(Long requestId) { return repository.findById(requestId).map(this::map).orElse(null); }
    public List<Request> inbox(Long targetId) { return repository.findAllByTargetIdOrderByIdDesc(targetId).stream().map(this::map).toList(); }
    public List<Request> outgoing(Long requesterId) { return repository.findAllByRequesterIdOrderByIdDesc(requesterId).stream().map(this::map).toList(); }
    public boolean delete(Long requestId) { if (!repository.existsById(requestId)) return false; repository.deleteById(requestId); return true; }
    public boolean cancel(Long requesterId, Long targetId) { return repository.deleteByRequesterIdAndTargetId(requesterId, targetId) > 0; }
    private Request map(FollowRequest request) { return new Request(request.getId(), request.getRequesterId(), request.getTargetId()); }
}
