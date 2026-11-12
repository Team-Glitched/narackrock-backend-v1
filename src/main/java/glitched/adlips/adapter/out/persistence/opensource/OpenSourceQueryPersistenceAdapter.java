package glitched.adlips.adapter.out.persistence.opensource;

import glitched.adlips.application.opensource.OpenSourceQueryPort;
import glitched.adlips.domain.opensource.OpenSourcePublication;
import static glitched.adlips.adapter.out.persistence.opensource.OpenSourceInteractionSql.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OpenSourceQueryPersistenceAdapter implements OpenSourceQueryPort {
    private static final String COMPLETE = """
            s.owner_id IS NOT NULL AND s.media_file_id IS NOT NULL AND s.title IS NOT NULL
            AND s.created_at IS NOT NULL AND s.updated_at IS NOT NULL AND s.deleted_at IS NULL
            """;
    private static final String PUBLIC = COMPLETE + " AND s.is_public = true AND u.deleted_at IS NULL";
    private static final String SELECT = "SELECT "
            +count("SOURCE","s.id","LIKE")+" AS like_count, "+count("SOURCE","s.id","DISLIKE")+" AS dislike_count, "
            +reacted("SOURCE","s.id","LIKE")+" AS is_liked, "+reacted("SOURCE","s.id","DISLIKE")+" AS is_disliked, "
            +"(SELECT COUNT(*) FROM open_source_comments c WHERE c.open_source_id=s.id AND "+visibleComment("c")+notHidden("c")+") AS comment_count, "
            +"""
            s.id, s.title, s.description, s.owner_id, s.created_at,
                   thumb.file_url AS thumbnail_url, COALESCE(p.nickname, '사용자') AS nickname,
                   avatar.file_url AS profile_image_url,
                   EXISTS (SELECT 1 FROM open_source_saves saved
                           WHERE saved.open_source_id = s.id AND saved.user_id = :viewer) AS is_saved,
                   CASE WHEN audio.status = 'READY' AND audio.file_type = 'AUDIO'
                             AND audio.owner_id = s.owner_id AND audio.file_url IS NOT NULL
                        THEN true ELSE false END AS can_preview
              FROM open_sources s
              JOIN users u ON u.id = s.owner_id
         LEFT JOIN profiles p ON p.user_id = s.owner_id
         LEFT JOIN media_files thumb ON thumb.id = s.thumbnail_media_file_id
                   AND thumb.owner_id = s.owner_id AND thumb.status = 'READY' AND thumb.file_type = 'IMAGE'
         LEFT JOIN media_files avatar ON avatar.id = p.profile_image_file_id
                   AND avatar.owner_id = s.owner_id AND avatar.status = 'READY' AND avatar.file_type = 'IMAGE'
         LEFT JOIN media_files audio ON audio.id = s.media_file_id
            """;
    private final NamedParameterJdbcTemplate jdbc;

    public OpenSourceQueryPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Item> list(Long viewerId, String keyword, int offset, int limit) {
        var parameters = parameters(viewerId, offset, limit);
        String search = "";
        if (keyword != null) {
            search = " AND (LOWER(s.title) LIKE LOWER(:keyword) ESCAPE '!' OR LOWER(s.description) LIKE LOWER(:keyword) ESCAPE '!')";
            parameters.addValue("keyword", "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        return jdbc.query(SELECT + " WHERE " + PUBLIC + search
                        + " ORDER BY s.created_at DESC, s.id DESC LIMIT :limit OFFSET :offset",
                parameters, (rs, row) -> item(rs));
    }

    @Override
    public Optional<Item> detail(Long viewerId, Long openSourceId) {
        return jdbc.query(SELECT + " WHERE " + PUBLIC + " AND s.id = :id",
                Map.of("viewer", viewerId, "id", openSourceId), (rs, row) -> item(rs)).stream().findFirst();
    }

    @Override
    public List<SavedItem> saved(Long viewerId, int offset, int limit) {
        String sql = "SELECT b.open_source_id AS saved_source_id, b.saved_at, visible.* FROM open_source_saves b LEFT JOIN ("
                + SELECT + " WHERE " + PUBLIC + ") visible ON visible.id = b.open_source_id"
                + " WHERE b.user_id = :viewer ORDER BY b.saved_at DESC, b.open_source_id DESC LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, parameters(viewerId, offset, limit), (rs, row) ->
                new SavedItem(rs.getLong("saved_source_id"), instant(rs, "saved_at"),
                        rs.getObject("id") == null ? null : item(rs)));
    }

    @Override
    public List<OpenSourcePublication> publications(Long ownerId, int offset, int limit) {
        return jdbc.query("SELECT s.* FROM open_sources s WHERE " + COMPLETE
                        + " AND s.owner_id = :viewer ORDER BY s.created_at DESC, s.id DESC LIMIT :limit OFFSET :offset",
                parameters(ownerId, offset, limit), (rs, row) -> new OpenSourcePublication(
                        rs.getLong("id"), rs.getLong("owner_id"), rs.getString("title"), rs.getString("description"),
                        rs.getLong("media_file_id"), rs.getObject("thumbnail_media_file_id", Long.class), rs.getBoolean("is_public"),
                        instant(rs, "created_at"), instant(rs, "updated_at"), null));
    }

    private MapSqlParameterSource parameters(Long viewerId, int offset, int limit) {
        return new MapSqlParameterSource("viewer", viewerId).addValue("offset", offset).addValue("limit", limit);
    }
    private Item item(ResultSet rs) throws SQLException {
        return new Item(rs.getLong("id"), rs.getString("title"), rs.getString("description"), rs.getString("thumbnail_url"),
                new Writer(rs.getLong("owner_id"), rs.getString("nickname"), rs.getString("profile_image_url")),
                rs.getBoolean("is_saved"), instant(rs, "created_at"), rs.getBoolean("can_preview"), rs.getBoolean("is_liked"), rs.getBoolean("is_disliked"),
                rs.getLong("like_count"), rs.getLong("dislike_count"), rs.getLong("comment_count"));
    }
    private Instant instant(ResultSet rs, String name) throws SQLException {
        return rs.getTimestamp(name).toInstant();
    }
}
