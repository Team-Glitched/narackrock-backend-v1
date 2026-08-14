package glitched.adlips.application.user.relation.usecase;

import glitched.adlips.application.user.relation.dto.request.UserSearchRequest;
import glitched.adlips.application.user.relation.dto.response.UserSearchResponse;
import glitched.adlips.application.user.relation.dto.response.UserSearchResponse.SearchUserResponse;
import glitched.adlips.application.user.relation.model.PageResult;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.Profile;
import java.util.List;
public class UserSearchUseCase {
    private static final int MAX_PAGE_SIZE = 100;

    private final ProfileQueryPort profileQuery;
    private final FollowRepositoryPort followRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final TransactionRunner transactionRunner;

    public UserSearchUseCase(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository
    ) {
        this(profileQuery, followRepository, mediaFileRepository, TransactionRunner.direct());
    }

    public UserSearchUseCase(
            ProfileQueryPort profileQuery,
            FollowRepositoryPort followRepository,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        this.profileQuery = profileQuery;
        this.followRepository = followRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.transactionRunner = transactionRunner;
    }

    public UserSearchResponse execute(UserSearchRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private UserSearchResponse executeInternal(UserSearchRequest request) {
        Long requesterId = request.requesterId();
        String keyword = request.keyword();
        int page = request.page();
        int size = request.size();
        validatePage(page, size);
        if (keyword == null || keyword.isBlank()) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "검색어는 필수 입력 사항입니다.");
        }
        String normalizedKeyword = keyword.trim();
        PageResult<Profile> result = profileQuery.search(normalizedKeyword, page, size);
        List<SearchUserResponse> users = result.content().stream()
                .map(profile -> new SearchUserResponse(
                        profile.getUserId(),
                        profile.getNickname(),
                        imageUrl(profile),
                        profile.getPrimaryInstrument(),
                        profile.getExplanation(),
                        !profile.getUserId().equals(requesterId)
                                && followRepository.exists(requesterId, profile.getUserId())
                ))
                .toList();
        return new UserSearchResponse(normalizedKeyword, result.totalCount(), users);
    }

    private String imageUrl(Profile profile) {
        if (profile.getProfileImageFileId() == null) {
            return null;
        }
        return mediaFileRepository.findById(profile.getProfileImageFileId())
                .map(MediaFile::getFileUrl)
                .orElse(null);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "페이지 번호와 크기를 확인해 주세요.");
        }
    }
}
