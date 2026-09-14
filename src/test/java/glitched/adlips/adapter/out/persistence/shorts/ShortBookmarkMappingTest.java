package glitched.adlips.adapter.out.persistence.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.domain.shorts.ShortBookmark;
import jakarta.persistence.Table;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ShortBookmarkMappingTest {

    @Test
    void ERD의_사용자_폴더_인덱스를_매핑한다() {
        Table table = ShortBookmark.class.getAnnotation(Table.class);

        assertThat(Arrays.stream(table.indexes()))
                .anySatisfy(index -> assertThat(index.columnList())
                        .isEqualTo("user_id, folder_id"));
    }
}
