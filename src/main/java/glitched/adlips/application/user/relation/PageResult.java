package glitched.adlips.application.user.relation;

import java.util.List;

public record PageResult<T>(List<T> content, long totalCount) {
    public PageResult {
        content = List.copyOf(content);
    }
}
