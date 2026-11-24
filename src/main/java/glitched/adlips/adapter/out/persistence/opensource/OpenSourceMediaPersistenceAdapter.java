package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.OpenSourceMediaPort;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OpenSourceMediaPersistenceAdapter implements OpenSourceMediaPort {
    private final JdbcTemplate jdbc;
    public OpenSourceMediaPersistenceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Optional<Source> findAvailable(Long id) {
        return jdbc.query("""
                SELECT s.owner_id,s.media_file_id,s.title,s.instrument FROM open_sources s JOIN users u ON u.id=s.owner_id
                WHERE s.id=? AND s.is_public=true AND s.deleted_at IS NULL AND u.deleted_at IS NULL
                AND s.title IS NOT NULL AND s.media_file_id IS NOT NULL
                AND s.created_at IS NOT NULL AND s.updated_at IS NOT NULL
                """, (rs,row) -> new Source(
                rs.getLong("owner_id"),
                rs.getLong("media_file_id"),
                rs.getString("title"),
                rs.getString("instrument")), id)
                .stream().findFirst();
    }

    @Override
    public Optional<Long> findOwnerId(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return jdbc.query("SELECT owner_id FROM open_sources WHERE id = ?",
                (rs, row) -> rs.getLong("owner_id"), id).stream().findFirst();
    }
}
