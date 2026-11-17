package glitched.adlips.application.community;

import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;

final class GalleryProposalAdminGuard {

    private GalleryProposalAdminGuard() {
    }

    static User requireAdmin(UserRepositoryPort userRepository, Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new GalleryProposalApplicationException(
                        GalleryProposalErrorCode.FORBIDDEN, "운영자 어드민만 커뮤니티 개설 요청을 처리할 수 있습니다."));
    }
}
