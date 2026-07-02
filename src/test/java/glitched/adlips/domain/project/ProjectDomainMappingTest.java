package glitched.adlips.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import glitched.adlips.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectDomainMappingTest {

    @Test
    void mapsCompositionEntitiesToTheLatestErdTables() {
        assertTable(Project.class, "projects");
        assertTable(ProjectMember.class, "project_members");
        assertTable(ProjectContribution.class, "project_contributions");
        assertTable(ProjectContributionItem.class, "project_contribution_items");
        assertTable(ProjectTrack.class, "project_tracks");
        assertTable(ProjectClip.class, "project_clips");
        assertTable(ProjectExport.class, "project_exports");
        assertTable(ProjectActivityLog.class, "project_activity_logs");
    }

    @Test
    void mapsTheFieldsThatWereMissingFromTheLegacyLayerModel() {
        assertColumns(Project.class, Map.ofEntries(
                Map.entry("albumImageFileId", "album_image_file_id"),
                Map.entry("timeSignatureNumerator", "time_signature_numerator"),
                Map.entry("timeSignatureDenominator", "time_signature_denominator"),
                Map.entry("ppq", "ppq"),
                Map.entry("maxDurationMs", "max_duration_ms"),
                Map.entry("majorVersion", "major_version"),
                Map.entry("minorVersion", "minor_version")
        ));
        assertColumns(ProjectTrack.class, Map.ofEntries(
                Map.entry("project", "project_id"),
                Map.entry("owner", "owner_id"),
                Map.entry("mediaFileId", "media_file_id"),
                Map.entry("approvalStatus", "approval_status"),
                Map.entry("isDeleted", "is_deleted")
        ));
        assertColumns(ProjectClip.class, Map.ofEntries(
                Map.entry("project", "project_id"),
                Map.entry("track", "track_id"),
                Map.entry("owner", "owner_id"),
                Map.entry("mediaFileId", "media_file_id"),
                Map.entry("waveformFileId", "waveform_file_id"),
                Map.entry("midiNotes", "midi_notes"),
                Map.entry("startTick", "start_tick"),
                Map.entry("durationTick", "duration_tick"),
                Map.entry("clipOffsetMs", "clip_offset_ms"),
                Map.entry("approvalStatus", "approval_status")
        ));
        assertColumns(ProjectContribution.class, Map.ofEntries(
                Map.entry("baseMajorVersion", "base_major_version"),
                Map.entry("baseMinorVersion", "base_minor_version")
        ));
        assertColumns(ProjectExport.class, Map.ofEntries(
                Map.entry("projectMajorVersion", "project_major_version"),
                Map.entry("projectMinorVersion", "project_minor_version"),
                Map.entry("mediaFileId", "media_file_id")
        ));
        assertNoField(ProjectTrack.class, "version");
        assertNoField(ProjectClip.class, "version");
        assertNoField(ProjectContributionItem.class, "submittedVersion");
        assertNoField(ProjectExport.class, "exportType");
        assertNoField(ProjectExport.class, "targetShortStatus");
        assertNoField(ProjectExport.class, "shortsId");
    }

    @Test
    void initializesAProjectWithTheErdAndApiDefaults() {
        Project project = new Project(
                new User("composer@example.com"), "새 프로젝트", "설명", 701L);

        assertThat(project.getTitle()).isEqualTo("새 프로젝트");
        assertThat(project.getBpm()).isEqualTo(120);
        assertThat(project.getTimeSignatureNumerator()).isEqualTo(4);
        assertThat(project.getTimeSignatureDenominator()).isEqualTo(4);
        assertThat(project.getPpq()).isEqualTo(480);
        assertThat(project.getMaxDurationMs()).isEqualTo(60_000);
        assertThat(project.getMajorVersion()).isEqualTo(1);
        assertThat(project.getMinorVersion()).isEqualTo(1);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(project.isPublic()).isFalse();
    }

    private static void assertNoField(Class<?> entityType, String fieldName) {
        assertThatThrownBy(() -> entityType.getDeclaredField(fieldName))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void exposesEveryStatusAndTypeDefinedByTheErd() {
        assertThat(ProjectMemberRole.values()).containsExactly(
                ProjectMemberRole.OWNER, ProjectMemberRole.EDITOR, ProjectMemberRole.VIEWER);
        assertThat(ClipSourceType.values()).containsExactly(
                ClipSourceType.RECORDING, ClipSourceType.FILE_UPLOAD,
                ClipSourceType.MIDI_INPUT, ClipSourceType.IMPORTED);
        assertThat(ContributionItemStatus.values()).containsExactly(
                ContributionItemStatus.PENDING, ContributionItemStatus.APPROVED,
                ContributionItemStatus.REJECTED, ContributionItemStatus.CONFLICTED);
        assertThat(ExportStatus.values()).containsExactly(
                ExportStatus.QUEUED, ExportStatus.PROCESSING,
                ExportStatus.COMPLETED, ExportStatus.FAILED);
    }

    private static void assertTable(Class<?> entityType, String expectedName) {
        assertThat(entityType.getAnnotation(Table.class))
                .extracting(Table::name)
                .isEqualTo(expectedName);
    }

    private static void assertColumns(Class<?> entityType, Map<String, String> expectedColumns) {
        expectedColumns.forEach((fieldName, columnName) ->
                assertThat(columnName(entityType, fieldName)).isEqualTo(columnName));
    }

    private static String columnName(Class<?> entityType, String fieldName) {
        try {
            Field field = entityType.getDeclaredField(fieldName);
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                return column.name();
            }
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            return joinColumn == null ? null : joinColumn.name();
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing field: " + entityType.getSimpleName() + "." + fieldName, exception);
        }
    }
}
