package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.dto.request.FollowCreateRequest;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRequestPort;
import java.util.List;

public class FollowRequestUseCase {
    public record Result(String status, Long requestId) {}
    private final FollowRequestPort requests;
    private final FollowRepositoryPort follows;
    private final ProfileRepositoryPort profiles;
    private final FollowCreateUseCase createFollow;
    private final TransactionRunner transactions;

    public FollowRequestUseCase(FollowRequestPort requests, FollowRepositoryPort follows, ProfileRepositoryPort profiles, FollowCreateUseCase createFollow) {
        this(requests, follows, profiles, createFollow, TransactionRunner.direct());
    }
    public FollowRequestUseCase(FollowRequestPort requests, FollowRepositoryPort follows, ProfileRepositoryPort profiles, FollowCreateUseCase createFollow, TransactionRunner transactions) {
        this.requests = requests; this.follows = follows; this.profiles = profiles; this.createFollow = createFollow; this.transactions = transactions;
    }
    public Result follow(Long requesterId, Long targetId) {
        return transactions.required(() -> {
            if (requesterId == null || requesterId.equals(targetId)) throw error(UserErrorCode.CANNOT_FOLLOW_SELF, "본인을 팔로우할 수 없습니다.");
            var profile = profiles.findByUserId(targetId).orElseThrow(() -> error(UserErrorCode.PROFILE_NOT_FOUND, "프로필이 없습니다."));
            if (follows.exists(requesterId, targetId)) throw error(UserErrorCode.ALREADY_FOLLOWING, "이미 팔로우 중입니다.");
            if (requests.exists(requesterId, targetId)) throw error(UserErrorCode.ALREADY_FOLLOWING, "이미 요청했습니다.");
            if (!profile.isPrivate()) { createFollow.execute(new FollowCreateRequest(requesterId, targetId)); return new Result("FOLLOWING", null); }
            return new Result("PENDING", requests.create(requesterId, targetId));
        });
    }
    public void accept(Long targetId, Long requestId) { review(targetId, requestId, true); }
    public void reject(Long targetId, Long requestId) { review(targetId, requestId, false); }
    private void review(Long targetId, Long requestId, boolean accept) {
        transactions.required(() -> {
            var request = requireOwned(targetId, requestId);
            if (accept) createFollow.execute(new FollowCreateRequest(request.requesterId(), targetId));
            requests.delete(requestId);
            return null;
        });
    }
    public boolean cancel(Long requesterId, Long targetId) { return transactions.required(() -> requests.cancel(requesterId, targetId)); }
    public List<FollowRequestPort.Request> inbox(Long targetId) { return transactions.readOnly(() -> requests.inbox(targetId)); }
    public List<FollowRequestPort.Request> outgoing(Long requesterId) { return transactions.readOnly(() -> requests.outgoing(requesterId)); }
    private FollowRequestPort.Request requireOwned(Long targetId, Long requestId) {
        var request = requests.find(requestId);
        if (request == null) throw error(UserErrorCode.FOLLOW_NOT_FOUND, "팔로우 요청이 없습니다.");
        if (!request.targetId().equals(targetId)) throw error(UserErrorCode.UNAUTHORIZED_ACCESS, "요청을 처리할 권한이 없습니다.");
        return request;
    }
    private UserApplicationException error(UserErrorCode code, String message) { return new UserApplicationException(code, message); }
}
