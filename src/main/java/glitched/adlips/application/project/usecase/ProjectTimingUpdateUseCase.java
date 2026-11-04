package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.dto.response.ProjectTimingResponse;
import glitched.adlips.application.project.dto.response.ProjectVersionResponse;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.domain.project.*;
import java.util.List;

public class ProjectTimingUpdateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectMemberRepositoryPort members;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final EditorDraftRepositoryPort drafts;
    private final ProjectExportRepositoryPort exports;
    private final TransactionRunner transactions;

    public ProjectTimingUpdateUseCase(ProjectRepositoryPort projects, ProjectMemberRepositoryPort members,
                                      ProjectTrackRepositoryPort tracks, ProjectClipRepositoryPort clips,
                                      EditorDraftRepositoryPort drafts, ProjectExportRepositoryPort exports,
                                      TransactionRunner transactions) {
        this.projects=projects; this.members=members; this.tracks=tracks; this.clips=clips;
        this.drafts=drafts; this.exports=exports; this.transactions=transactions;
    }

    public ProjectTimingResponse execute(Long projectId, Long userId, int bpm, int numerator, int denominator) {
        return transactions.required(() -> {
            // The same project lock serializes timing changes with draft saves/commits.
            var project=projects.findByIdAndDeletedAtIsNullForUpdate(projectId).orElseThrow(() ->
                    error(ProjectErrorCode.PROJECT_NOT_FOUND,"곡을 찾을 수 없습니다."));
            members.findByProjectIdAndUserId(projectId,userId)
                    .filter(member -> member.getRole()==ProjectMemberRole.OWNER)
                    .orElseThrow(() -> error(ProjectErrorCode.PROJECT_ACCESS_DENIED,"원곡자만 곡 설정을 변경할 수 있습니다."));
            if (project.getStatus()!=ProjectStatus.DRAFT && project.getStatus()!=ProjectStatus.IN_PROGRESS) {
                throw error(ProjectErrorCode.PROJECT_ACCESS_DENIED,"완료된 곡의 설정은 변경할 수 없습니다.");
            }
            if (bpm<30 || bpm>300 || numerator<1 || numerator>12
                    || (denominator!=2 && denominator!=4 && denominator!=8 && denominator!=16)) {
                throw error(ProjectErrorCode.VALIDATION_ERROR,"BPM은 30~300, 박자표는 올바른 값을 입력해 주세요.");
            }
            boolean tempoChanged=project.getBpm()!=bpm;
            if (tempoChanged) {
                long maxTick=(long)project.getMaxDurationMs()*bpm*project.getPpq()/60_000;
                List<ProjectClip> allClips=tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(projectId)
                        .stream().flatMap(track -> clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(track.getId()).stream()).toList();
                for (var clip:allClips) {
                    long durationTick=clip.getClipType()==ClipType.MIDI ? clip.getDurationTick()
                            : ((long)clip.getDurationMs()*bpm*project.getPpq()+59_999)/60_000;
                    if ((long)clip.getStartTick()+durationTick>maxTick) throw outsideTimeline();
                }
                for (var draft:drafts.findActiveByProject(projectId)) {
                    if ((long)draft.content().startTick()+draft.content().durationTick()>maxTick) throw outsideTimeline();
                }
                project.updateTiming(bpm,numerator,denominator);
                allClips.forEach(ProjectClip::refreshProjectTiming);
            } else if (!project.updateTiming(bpm,numerator,denominator)) {
                return response(project);
            }
            if (project.isPublic()) exports.save(new ProjectExport(project,project.getOwner()));
            return response(project);
        });
    }

    private ProjectTimingResponse response(Project project) {
        return new ProjectTimingResponse(project.getBpm(),project.getTimeSignatureNumerator(),project.getTimeSignatureDenominator(),
                project.getMaxTick(),new ProjectVersionResponse(project.getMajorVersion(),project.getMinorVersion(),project.getDisplayVersion()));
    }
    private ProjectApplicationException outsideTimeline() {
        return error(ProjectErrorCode.PROJECT_DURATION_LIMIT_EXCEEDED,"이 BPM에서는 기존 클립이나 작업 중인 초안이 1분을 넘습니다. 노트 길이나 위치를 먼저 조절해 주세요.");
    }
    private ProjectApplicationException error(ProjectErrorCode code,String message) { return new ProjectApplicationException(code,message); }
}
