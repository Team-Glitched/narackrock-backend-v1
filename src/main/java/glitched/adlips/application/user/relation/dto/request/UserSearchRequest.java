package glitched.adlips.application.user.relation.dto.request;

public record UserSearchRequest(Long requesterId, String keyword, int page, int size) {
}
