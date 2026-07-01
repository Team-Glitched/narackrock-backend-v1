package glitched.adlips.application.user.relation.model;

import java.util.List;

public record PageResult<T>(List<T> content, long totalCount) {
    public PageResult {
        content = List.copyOf(content);
    }
}
