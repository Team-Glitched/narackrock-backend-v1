package glitched.adlips.application.project.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.ProjectErrorCode;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.domain.project.*;
import java.util.Objects;

public class TrackInstrumentUpdateUseCase {
    private final ProjectRepositoryPort projects;
    private final ProjectTrackRepositoryPort tracks;
    private final ProjectClipRepositoryPort clips;
    private final EditorDraftRepositoryPort drafts;
    private final ProjectExportRepositoryPort exports;
    private final TransactionRunner transactions;
    public TrackInstrumentUpdateUseCase(ProjectRepositoryPort projects,ProjectTrackRepositoryPort tracks,
            ProjectClipRepositoryPort clips,EditorDraftRepositoryPort drafts,ProjectExportRepositoryPort exports,TransactionRunner transactions) {
        this.projects=projects;this.tracks=tracks;this.clips=clips;this.drafts=drafts;this.exports=exports;this.transactions=transactions;
    }
    public String execute(Long projectId,Long trackId,Long userId,String instrument) {
        return transactions.required(() -> {
            // All role reservations share this lock with track creation.
            var project=projects.findByIdAndDeletedAtIsNullForUpdate(projectId).orElseThrow(() -> error(ProjectErrorCode.PROJECT_NOT_FOUND,"곡을 찾을 수 없습니다."));
            var track=tracks.findTrackByIdAndIsDeletedFalse(trackId).orElseThrow(() -> error(ProjectErrorCode.TRACK_NOT_FOUND,"레이어를 찾을 수 없습니다."));
            if(!track.belongsTo(project) || !userId.equals(track.getOwner().getId()) || project.getStatus()==ProjectStatus.COMPLETED
                    || (track.getApprovalStatus()!=ApprovalStatus.DRAFT && track.getApprovalStatus()!=ApprovalStatus.APPROVED)) {
                throw error(ProjectErrorCode.TRACK_EDIT_DENIED,"본인의 편집 가능한 레이어만 악기를 바꿀 수 있습니다.");
            }
            String next=InstrumentFamily.normalize(instrument);
            var family=InstrumentFamily.of(next);
            if(family==null) throw error(ProjectErrorCode.VALIDATION_ERROR,"악기나 보컬 역할을 선택해 주세요.");
            if(Objects.equals(track.getInstrument(),next)) return next;
            if(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(projectId).stream()
                    .anyMatch(other -> other!=track && !Objects.equals(other.getId(),trackId)
                            && other.getApprovalStatus()!=ApprovalStatus.REJECTED && InstrumentFamily.of(other.getInstrument())==family)) {
                throw error(ProjectErrorCode.INSTRUMENT_FAMILY_TAKEN,family.label()+" 종류는 팀원이 이미 사용 중입니다. 다른 종류를 선택해 주세요.");
            }
            var savedClips=clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(trackId);
            boolean hasMidi=savedClips.stream().anyMatch(clip -> clip.getClipType()==ClipType.MIDI);
            var activeDrafts=drafts.findActiveByProjectAndOwner(projectId,userId).stream().filter(d -> d.trackId()==trackId).toList();
            if(InstrumentFamily.audioOnly(next) && (hasMidi || activeDrafts.stream().anyMatch(d -> !d.content().midiNotes().isEmpty()))) {
                throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA,"노트가 있는 레이어에는 연주할 수 있는 악기를 선택해 주세요.");
            }
            if(next.equals("drum-kit") && (savedClips.stream().flatMap(clip -> clip.getMidiNotes().stream()).anyMatch(n -> n.pitch()<35 || n.pitch()>81)
                    || activeDrafts.stream().flatMap(d -> d.content().midiNotes().stream()).anyMatch(n -> n.pitch()<35 || n.pitch()>81))) {
                throw error(ProjectErrorCode.INVALID_MIDI_NOTE_DATA,"현재 노트에 드럼 음역(35~81)을 벗어나는 음이 있어요. 노트를 먼저 조절해 주세요.");
            }
            boolean changed=track.changeInstrument(next);
            if(changed && project.isPublic() && track.getApprovalStatus()==ApprovalStatus.APPROVED) exports.save(new ProjectExport(project,project.getOwner()));
            return track.getInstrument();
        });
    }
    private ProjectApplicationException error(ProjectErrorCode code,String message) { return new ProjectApplicationException(code,message); }
}
