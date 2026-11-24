package glitched.adlips.adapter.out.persistence.project;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ProjectTrackJpaRepositoryTest {
    @Autowired ProjectTrackJpaRepository tracks;
    @Autowired JdbcTemplate jdbc;

    @Test
    void findsOwningProjectIdOnlyForLiveTracks() {
        jdbc.update("INSERT INTO users(id,email,role,created_at) VALUES(97001,'track-project@test.local','USER',CURRENT_TIMESTAMP)");
        jdbc.update("""
                INSERT INTO projects(id,owner_id,title,album_image_file_id,bpm,time_signature_numerator,
                    time_signature_denominator,ppq,max_duration_ms,major_version,minor_version,status,
                    is_public,created_at,updated_at)
                VALUES(97301,97001,'곡',97999,120,4,4,480,60000,1,1,'IN_PROGRESS',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO project_tracks(id,project_id,owner_id,name,instrument,volume,pan,sort_order,
                    is_muted,is_solo,approval_status,is_deleted,origin_open_source_id,created_at,updated_at)
                VALUES(97401,97301,97001,'샘플','drum-kit',100,0,0,false,false,'APPROVED',false,55,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                      (97402,97301,97001,'삭제됨','piano',100,0,1,false,false,'APPROVED',true,null,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);

        assertThat(tracks.findProjectIdByIdAndIsDeletedFalse(97401L)).contains(97301L);
        assertThat(tracks.findProjectIdByIdAndIsDeletedFalse(97402L)).isEmpty();
        assertThat(tracks.findProjectIdByIdAndIsDeletedFalse(97403L)).isEmpty();
    }
}
