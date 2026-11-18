package glitched.adlips.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.dto.request.TrackGetListRequest;
import glitched.adlips.application.project.usecase.TrackGetListUseCase;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackGetListUseCaseTest {
    @Test
    void returnsTimelineAndOwnerPermissions() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", "설명", 701L);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());

        var response = new TrackGetListUseCase(projects, members, tracks, clips, mock(MediaFileRepositoryPort.class), mock(ProfileRepositoryPort.class))
                .execute(new TrackGetListRequest(10L, 1L));

        assertThat(response.project().maxTick()).isEqualTo(57_600L);
        assertThat(response.viewerPermission().canExport()).isTrue();
        assertThat(response.tracks()).isEmpty();
    }

    @Test
    void returnsSavedMidiNotesForPianoRollReload() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", "설명", 701L);
        ProjectTrack track = mock(ProjectTrack.class);
        ProjectClip clip = mock(ProjectClip.class);
        MidiNote note = new MidiNote(60, 0, 120, 100, null, null);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L))
                .thenReturn(List.of(track));
        when(track.getId()).thenReturn(20L);
        when(track.getOwner()).thenReturn(owner);
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(20L))
                .thenReturn(List.of(clip));
        when(clip.getId()).thenReturn(30L);
        when(clip.getOwner()).thenReturn(owner);
        when(clip.getClipType()).thenReturn(ClipType.MIDI);
        when(clip.getMidiNotes()).thenReturn(List.of(note));

        var response = new TrackGetListUseCase(projects, members, tracks, clips,
                mock(MediaFileRepositoryPort.class), mock(ProfileRepositoryPort.class)).execute(new TrackGetListRequest(10L, 1L));

        assertThat(response.tracks().getFirst().clips().getFirst().midiNotes())
                .containsExactly(note);
        assertThat(response.tracks().getFirst().ownerProfileImageUrl()).isNull();
    }
    @Test
    void hidesOtherUsersEmptyTracksButKeepsOwnEmptyLayerEditable() {
        User owner = User.create("owner@example.com").withId(1L);
        User editor = User.create("editor@example.com").withId(2L);
        Project project = new Project(owner, "곡", "설명", 701L);
        ProjectTrack own = mock(ProjectTrack.class);
        ProjectTrack emptyOther = mock(ProjectTrack.class);
        ProjectTrack populated = mock(ProjectTrack.class);
        ProjectClip clip = mock(ProjectClip.class);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(new ProjectMember(project, editor, ProjectMemberRole.EDITOR)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L))
                .thenReturn(List.of(own, emptyOther, populated));
        when(own.getId()).thenReturn(20L);
        when(own.getOwner()).thenReturn(editor);
        when(emptyOther.getId()).thenReturn(21L);
        when(emptyOther.getOwner()).thenReturn(owner);
        when(populated.getId()).thenReturn(22L);
        when(populated.getOwner()).thenReturn(owner);
        when(clip.getOwner()).thenReturn(owner);
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(22L)).thenReturn(List.of(clip));

        var response = new TrackGetListUseCase(projects, members, tracks, clips,
                mock(MediaFileRepositoryPort.class), mock(ProfileRepositoryPort.class)).execute(new TrackGetListRequest(10L, 2L));

        assertThat(response.tracks()).extracting(t -> t.trackId()).containsExactly(20L, 22L);
    }

    @Test
    void includesLayerOwnersProfilePhotoWithoutRequiringPublicProfile() {
        User owner = User.create("owner@example.com").withId(1L);
        Project project = new Project(owner, "곡", "설명", 701L);
        ProjectTrack track = mock(ProjectTrack.class);
        ProjectRepositoryPort projects = mock(ProjectRepositoryPort.class);
        ProjectMemberRepositoryPort members = mock(ProjectMemberRepositoryPort.class);
        ProjectTrackRepositoryPort tracks = mock(ProjectTrackRepositoryPort.class);
        ProjectClipRepositoryPort clips = mock(ProjectClipRepositoryPort.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        MediaFileRepositoryPort media = mock(MediaFileRepositoryPort.class);
        MediaFile photo = mock(MediaFile.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.OWNER)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(track));
        when(track.getOwner()).thenReturn(owner);
        when(track.getId()).thenReturn(20L);
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(
                Profile.create(1L, "기타 연주자").changePrivacy(true).changeProfileImage(42L)));
        when(media.findById(42L)).thenReturn(Optional.of(photo));
        when(photo.getFileUrl()).thenReturn("https://files.test/owner.png");

        var response = new TrackGetListUseCase(projects, members, tracks, clips, media, profiles)
                .execute(new TrackGetListRequest(10L, 1L));

        assertThat(response.tracks().getFirst().ownerProfileImageUrl()).isEqualTo("https://files.test/owner.png");
    }

}
