package glitched.adlips.application.user.port.out;

import glitched.adlips.application.user.PageResult;
import glitched.adlips.domain.user.Profile;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProfileQueryPort {
    List<Profile> findAllByUserIds(Collection<Long> userIds);

    PageResult<Profile> search(String keyword, int page, int size);

    PageResult<Profile> findPopularExcluding(Set<Long> excludedIds, int page, int size);
}
