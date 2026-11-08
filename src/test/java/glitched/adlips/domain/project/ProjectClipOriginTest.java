package glitched.adlips.domain.project;
import static org.assertj.core.api.Assertions.assertThat;
import glitched.adlips.domain.user.User;
import org.junit.jupiter.api.Test;
class ProjectClipOriginTest {
    @Test void replacingAudioClearsTheOldSourceButTimelineEditsKeepIt(){
        User owner=User.create("origin-test@example.invalid");
        Project project=new Project(owner,"출처 검증",null,701L);
        ProjectTrack track=new ProjectTrack(project,owner,"기타","acoustic-guitar",0);
        ProjectClip clip=ProjectClip.createAudio(project,track,owner,ClipSourceType.IMPORTED,801L,null,0,960,0,500,0);
        clip.attributeOpenSource(7L);
        clip.updateAudio(ClipSourceType.IMPORTED,801L,null,960,960,0,500);
        assertThat(clip.getOriginOpenSourceId()).isEqualTo(7L);
        clip.updateAudio(ClipSourceType.RECORDING,802L,null,960,960,0,500);
        assertThat(clip.getOriginOpenSourceId()).isNull();
    }
}
