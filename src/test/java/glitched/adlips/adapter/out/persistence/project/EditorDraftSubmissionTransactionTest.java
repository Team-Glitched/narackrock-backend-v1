package glitched.adlips.adapter.out.persistence.project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import glitched.adlips.adapter.out.transaction.SpringTransactionRunner;
import glitched.adlips.application.project.dto.request.*;
import glitched.adlips.application.project.port.out.*;
import glitched.adlips.application.project.usecase.*;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.project.*;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest(properties = "spring.datasource.name=editor-draft-transaction-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EditorDraftSubmissionTransactionTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EditorDraftJpaRepository repository;

    @Test void contributionFailureRollsBackBothNewClipAndDraftCommitReceipt() {
        var transactions = new SpringTransactionRunner(transactionManager);
        long[] ids = transactions.required(() -> {
            var user = User.create("transaction-draft@test.com"); em.persist(user);
            var project = new Project(user, "곡", null, 0L); em.persist(project);
            var track = new ProjectTrack(project, user, "피아노", "piano", 0); em.persist(track);
            return new long[]{ user.getId(), project.getId(), track.getId() };
        });
        long userId = ids[0], projectId = ids[1], trackId = ids[2];
        var projects = mock(ProjectRepositoryPort.class);
        var members = mock(ProjectMemberRepositoryPort.class);
        var tracks = mock(ProjectTrackRepositoryPort.class);
        var clips = mock(ProjectClipRepositoryPort.class);
        var users = mock(UserRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNullForUpdate(projectId)).thenAnswer(call ->
                Optional.of(em.find(Project.class, projectId, LockModeType.PESSIMISTIC_WRITE)));
        when(projects.findByIdAndDeletedAtIsNull(projectId)).thenAnswer(call -> Optional.of(em.find(Project.class, projectId)));
        when(users.findById(userId)).thenAnswer(call -> Optional.of(em.find(User.class, userId)));
        when(members.findByProjectIdAndUserId(projectId, userId)).thenAnswer(call -> Optional.of(
                new ProjectMember(em.find(Project.class, projectId), em.find(User.class, userId), ProjectMemberRole.OWNER)));
        when(tracks.findTrackByIdAndIsDeletedFalse(trackId)).thenAnswer(call -> Optional.of(em.find(ProjectTrack.class, trackId)));
        when(tracks.findByProjectIdAndIsDeletedFalseOrderBySortOrderAscIdAsc(projectId))
                .thenAnswer(call -> List.of(em.find(ProjectTrack.class, trackId)));
        when(clips.findByTrackIdAndIsDeletedFalseOrderByStartTickAscIdAsc(trackId)).thenAnswer(call ->
                em.createQuery("select c from ProjectClip c where c.track.id = :track", ProjectClip.class)
                        .setParameter("track", trackId).getResultList());
        when(clips.save(any(ProjectClip.class))).thenAnswer(call -> {
            ProjectClip clip = call.getArgument(0); em.persist(clip); return clip;
        });
        var drafts = new EditorDraftUseCase(projects, members, tracks, clips, users,
                new EditorDraftPersistenceAdapter(repository),
                new MidiClipCreateUseCase(projects, members, tracks, clips, users, transactions),
                new MidiClipSaveUseCase(clips, Clock.systemUTC(), transactions), transactions, Clock.systemUTC());
        UUID draftId = UUID.randomUUID();
        drafts.save(projectId, trackId, draftId, userId, new EditorDraftSaveRequest(0, UUID.randomUUID(), null,
                new EditorDraftSnapshot(0, 480, List.of()), null));
        var contributions = mock(ProjectContributionCreateUseCase.class);
        when(contributions.execute(any())).thenThrow(new IllegalStateException("submission failed"));
        var workflow = new EditorDraftWorkflowUseCase(drafts, projects, tracks, clips, contributions, transactions);

        assertThatThrownBy(() -> workflow.submit(projectId, userId, new EditorDraftBatchRequest(
                List.of(new EditorDraftBatchRequest.Reference(draftId, 1)), "멜로디", null)))
                .isInstanceOf(IllegalStateException.class);

        transactions.readOnly(() -> {
            assertThat(repository.findById(draftId).orElseThrow().toDomain().status()).isEqualTo(EditorDraft.Status.ACTIVE);
            assertThat(em.createQuery("select count(c) from ProjectClip c where c.track.id = :track", Long.class)
                    .setParameter("track", trackId).getSingleResult()).isZero();
            return null;
        });
    }
}
