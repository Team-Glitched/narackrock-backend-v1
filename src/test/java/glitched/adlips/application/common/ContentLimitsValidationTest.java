package glitched.adlips.application.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.CreatePostCommentUseCase;
import glitched.adlips.application.community.CreatePostUseCase;
import glitched.adlips.application.community.PostApplicationException;
import glitched.adlips.application.community.PostCommentApplicationException;
import glitched.adlips.application.community.RejectGalleryProposalUseCase;
import glitched.adlips.application.community.SanctionGalleryUserUseCase;
import glitched.adlips.application.community.UpdatePostCommentUseCase;
import glitched.adlips.application.community.UpdatePostUseCase;
import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryManagementPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.GalleryProposalPort;
import glitched.adlips.application.community.port.out.GallerySubscriptionPort;
import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.PostQueryPort;
import glitched.adlips.application.community.GetPostsUseCase;
import glitched.adlips.application.community.GalleryProposalApplicationException;
import glitched.adlips.application.community.GalleryModerationApplicationException;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.project.ProjectApplicationException;
import glitched.adlips.application.project.dto.request.ProjectContributionCreateRequest;
import glitched.adlips.application.project.dto.request.ProjectCreateRequest;
import glitched.adlips.application.project.dto.request.TrackCreateRequest;
import glitched.adlips.application.project.port.out.ProjectClipRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionItemRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectContributionRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectMemberRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectRepositoryPort;
import glitched.adlips.application.project.port.out.ProjectTrackRepositoryPort;
import glitched.adlips.application.project.usecase.ProjectContributionCreateUseCase;
import glitched.adlips.application.project.usecase.ProjectCreateUseCase;
import glitched.adlips.application.project.usecase.TrackCreateUseCase;
import glitched.adlips.application.report.port.out.ReportContentModerationPort;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.application.report.port.out.ReportSubmissionPort;
import glitched.adlips.application.report.port.out.ReportTarget;
import glitched.adlips.application.report.port.out.ReportTargetQueryPort;
import glitched.adlips.application.report.port.out.ReportUserRestrictionPort;
import glitched.adlips.application.report.usecase.RejectReportUseCase;
import glitched.adlips.application.report.usecase.ReportResolutionApplicationException;
import glitched.adlips.application.report.usecase.ReportSubmissionApplicationException;
import glitched.adlips.application.report.usecase.ResolveReportUseCase;
import glitched.adlips.application.report.usecase.SubmitReportUseCase;
import glitched.adlips.application.shorts.GetShortsUseCase;
import glitched.adlips.application.shorts.ShortCommentApplicationException;
import glitched.adlips.application.shorts.ShortReportApplicationException;
import glitched.adlips.application.shorts.ShortsCompositionApplicationException;
import glitched.adlips.application.shorts.ShortsPage;
import glitched.adlips.application.shorts.SubmitShortCommentReportUseCase;
import glitched.adlips.application.shorts.SubmitShortCommentUseCase;
import glitched.adlips.application.shorts.SubmitShortReportUseCase;
import glitched.adlips.application.shorts.UpdateShortCommentUseCase;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.application.shorts.port.out.ShortCommentReportPort;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import glitched.adlips.application.shorts.port.out.ShortReportPort;
import glitched.adlips.application.shorts.port.out.ShortsQueryPort;
import glitched.adlips.application.user.account.dto.request.UserSignupRequest;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.account.usecase.RefreshTokenManager;
import glitched.adlips.application.user.account.usecase.UserSignupUseCase;
import glitched.adlips.application.user.admin.dto.request.UserBanCreateRequest;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.application.user.admin.usecase.UserBanCreateUseCase;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.dto.request.UserProfileUpdateRequest;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.profile.usecase.ProfileUpdateUseCase;
import glitched.adlips.application.user.relation.dto.request.UserSearchRequest;
import glitched.adlips.application.user.relation.model.PageResult;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ProfileQueryPort;
import glitched.adlips.application.user.relation.usecase.UserSearchUseCase;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.common.TextSanitizer;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.project.ContributionChangeType;
import glitched.adlips.domain.project.ContributionTargetType;
import glitched.adlips.domain.project.Project;
import glitched.adlips.domain.project.ProjectClip;
import glitched.adlips.domain.project.ProjectMember;
import glitched.adlips.domain.project.ProjectMemberRole;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContentLimitsValidationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void codePointLengthCountsSurrogatePairsAsOneCharacter() {
        String twentyEmojis = "🎹".repeat(20);
        assertThat(twentyEmojis.length()).isEqualTo(40);
        assertThat(ContentLimits.codePointLength(twentyEmojis)).isEqualTo(20);
        assertThat(ContentLimits.exceeds(twentyEmojis, ContentLimits.NICKNAME_MAX_LENGTH)).isFalse();
        assertThat(ContentLimits.exceeds(twentyEmojis + "a", ContentLimits.NICKNAME_MAX_LENGTH)).isTrue();
    }

    @Test
    void profileAndSignupEnforceNicknameInstrumentAndBioLimits() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
        User user = User.create("u@example.com").withId(1L);
        Profile profile = Profile.create(1L, "old");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updateUseCase = new ProfileUpdateUseCase(users, profiles, CLOCK);
        // 20 emoji code points (40 UTF-16 chars) allowed; 21 rejected
        var ok = updateUseCase.execute(new UserProfileUpdateRequest(
                1L, "🎹".repeat(20), "a".repeat(250), "b".repeat(200)));
        assertThat(ok.nickname()).isEqualTo("🎹".repeat(20));
        assertThatThrownBy(() -> updateUseCase.execute(
                new UserProfileUpdateRequest(1L, "🎹".repeat(21), null, null)))
                .isInstanceOf(UserApplicationException.class);
        assertThatThrownBy(() -> updateUseCase.execute(
                new UserProfileUpdateRequest(1L, null, "a".repeat(251), null)))
                .isInstanceOf(UserApplicationException.class);
        assertThatThrownBy(() -> updateUseCase.execute(
                new UserProfileUpdateRequest(1L, null, null, "b".repeat(201))))
                .isInstanceOf(UserApplicationException.class);

        var signupUseCase = new UserSignupUseCase(
                mock(GoogleIdentityPort.class),
                mock(AccessTokenPort.class),
                users,
                mock(UserAuthProviderRepositoryPort.class),
                profiles,
                mock(RefreshTokenManager.class));
        assertThatThrownBy(() -> signupUseCase.execute(new UserSignupRequest("token", "a".repeat(21))))
                .isInstanceOf(UserApplicationException.class);
    }

    @Test
    void postsAndCommentsEnforceLimitsAcrossCreateAndUpdate() {
        PostPort postPort = mock(PostPort.class);
        GallerySubscriptionPort subs = mock(GallerySubscriptionPort.class);
        when(subs.lockGallery(10L)).thenReturn(true);
        when(subs.isSubscribed(1L, 10L)).thenReturn(true);
        when(postPort.save(any(), any(), any(), any(), any())).thenReturn(99L);

        var createPost = new CreatePostUseCase(postPort, subs, mock(MediaFileRepositoryPort.class), TransactionRunner.direct());
        assertThat(createPost.execute(10L, 1L, "🎹".repeat(40), "a".repeat(5000)).postId()).isEqualTo(99L);
        assertThatThrownBy(() -> createPost.execute(10L, 1L, "a".repeat(41), "ok"))
                .isInstanceOf(PostApplicationException.class);
        assertThatThrownBy(() -> createPost.execute(10L, 1L, "ok", "a".repeat(5001)))
                .isInstanceOf(PostApplicationException.class);

        var updatePost = new UpdatePostUseCase(postPort, TransactionRunner.direct());
        assertThatThrownBy(() -> updatePost.execute(99L, 1L, "a".repeat(41), "ok"))
                .isInstanceOf(PostApplicationException.class);
        assertThatThrownBy(() -> updatePost.execute(99L, 1L, "ok", "a".repeat(5001)))
                .isInstanceOf(PostApplicationException.class);
        verify(postPort, never()).findActivePost(99L);

        PostCommentPort postCommentPort = mock(PostCommentPort.class);
        var createPostComment = new CreatePostCommentUseCase(postCommentPort, (u, n) -> false, TransactionRunner.direct(), CLOCK);
        var updatePostComment = new UpdatePostCommentUseCase(postCommentPort, (u, n) -> false, TransactionRunner.direct(), CLOCK);
        assertThatThrownBy(() -> createPostComment.execute(1L, 1L, "a".repeat(3001), null))
                .isInstanceOf(PostCommentApplicationException.class);
        assertThatThrownBy(() -> updatePostComment.execute(1L, 1L, "a".repeat(3001)))
                .isInstanceOf(PostCommentApplicationException.class);
        verify(postCommentPort, never()).existsActivePost(any());

        ShortCommentPort shortCommentPort = mock(ShortCommentPort.class);
        var createShortComment = new SubmitShortCommentUseCase(shortCommentPort, (u, n) -> false, TransactionRunner.direct(), CLOCK);
        var updateShortComment = new UpdateShortCommentUseCase(shortCommentPort, TransactionRunner.direct());
        assertThatThrownBy(() -> createShortComment.execute(1L, 1L, "a".repeat(3001), null))
                .isInstanceOf(ShortCommentApplicationException.class);
        assertThatThrownBy(() -> updateShortComment.execute(1L, 1L, 1L, "a".repeat(3001)))
                .isInstanceOf(ShortCommentApplicationException.class);
        verify(shortCommentPort, never()).existsActiveShort(any());
    }

    @Test
    void projectsTracksContributionsAndGalleryRejectOversizedInputs() {
        User owner = User.create("owner@example.com").withId(1L);
        var projectCreate = new ProjectCreateUseCase(
                mock(UserRepositoryPort.class),
                mock(MediaFileRepositoryPort.class),
                mock(ProjectRepositoryPort.class),
                mock(ProjectMemberRepositoryPort.class));
        assertThatThrownBy(() -> projectCreate.execute(new ProjectCreateRequest(1L, "a".repeat(41), null, 0L)))
                .isInstanceOf(ProjectApplicationException.class);
        assertThatThrownBy(() -> projectCreate.execute(new ProjectCreateRequest(1L, "제목", "a".repeat(501), 0L)))
                .isInstanceOf(ProjectApplicationException.class);

        var trackCreate = new TrackCreateUseCase(
                mock(ProjectRepositoryPort.class),
                mock(ProjectMemberRepositoryPort.class),
                mock(ProjectTrackRepositoryPort.class),
                mock(UserRepositoryPort.class));
        assertThatThrownBy(() -> trackCreate.execute(new TrackCreateRequest(1L, 1L, "a".repeat(41), "PIANO", 0)))
                .isInstanceOf(ProjectApplicationException.class);

        var project = new Project(owner, "곡", null, 0L);
        var projects = mock(ProjectRepositoryPort.class);
        var members = mock(ProjectMemberRepositoryPort.class);
        var contributions = mock(ProjectContributionRepositoryPort.class);
        var users = mock(UserRepositoryPort.class);
        var tracks = mock(ProjectTrackRepositoryPort.class);
        var clips = mock(ProjectClipRepositoryPort.class);
        when(projects.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(new ProjectMember(project, owner, ProjectMemberRole.EDITOR)));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(contributions.findByProjectIdAndUserIdAndApprovalStatusInOrderByIdDesc(any(), any(), any(), any(int.class)))
                .thenReturn(List.of());
        ProjectClip clip = mock(ProjectClip.class);
        when(clip.getProject()).thenReturn(project);
        when(clip.getOwner()).thenReturn(owner);
        when(clip.getApprovalStatus()).thenReturn(glitched.adlips.domain.project.ApprovalStatus.DRAFT);
        when(clips.findClipByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(clip));

        var contribCreate = new ProjectContributionCreateUseCase(
                projects, members, contributions, mock(ProjectContributionItemRepositoryPort.class), tracks, clips, users);
        assertThatThrownBy(() -> contribCreate.execute(new ProjectContributionCreateRequest(
                10L, 1L, "a".repeat(121), null,
                List.of(new ProjectContributionCreateRequest.Item(ContributionChangeType.ADD, ContributionTargetType.CLIP, 20L)))))
                .isInstanceOf(ProjectApplicationException.class);

        assertThat(Gallery.normalizeName("🎹".repeat(20))).isEqualTo("🎹".repeat(20));
        assertThatThrownBy(() -> Gallery.normalizeName("🎹".repeat(21)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportsModerationBansAndSearchEnforceLimits() {
        User admin = User.restore(9L, "admin@example.com", UserRole.ADMIN, null);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(users.findById(9L)).thenReturn(Optional.of(admin));

        var submitReport = new SubmitReportUseCase(
                mock(ReportTargetQueryPort.class),
                mock(ReportSubmissionPort.class),
                mock(ReportUserRestrictionPort.class),
                CLOCK);
        assertThatThrownBy(() -> submitReport.execute(ReportTargetType.POST, 1L, 2L, "r".repeat(101), null))
                .isInstanceOf(ReportSubmissionApplicationException.class);
        assertThatThrownBy(() -> submitReport.execute(ReportTargetType.POST, 1L, 2L, "기타", "d".repeat(1001)))
                .isInstanceOf(ReportSubmissionApplicationException.class);

        var submitShortReport = new SubmitShortReportUseCase(mock(ShortReportPort.class));
        assertThatThrownBy(() -> submitShortReport.execute(1L, 2L, "기타", "d".repeat(1001)))
                .isInstanceOf(ShortReportApplicationException.class);

        var submitShortCommentReport = new SubmitShortCommentReportUseCase(
                mock(ShortCommentPort.class), mock(ShortCommentReportPort.class));
        assertThatThrownBy(() -> submitShortCommentReport.execute(1L, 2L, 3L, "기타", "d".repeat(1001)))
                .isInstanceOf(ShortCommentApplicationException.class);

        var resolveReport = new ResolveReportUseCase(
                mock(ReportResolutionPort.class), mock(ReportContentModerationPort.class),
                mock(UserBanRepositoryPort.class), users, CLOCK, TransactionRunner.direct());
        assertThatThrownBy(() -> resolveReport.execute(1L, 9L, "WARN_USER", "a".repeat(501)))
                .isInstanceOf(ReportResolutionApplicationException.class);

        var rejectReport = new RejectReportUseCase(
                mock(ReportResolutionPort.class), users, CLOCK, TransactionRunner.direct());
        assertThatThrownBy(() -> rejectReport.execute(1L, 9L, "a".repeat(501)))
                .isInstanceOf(ReportResolutionApplicationException.class);

        var rejectProposal = new RejectGalleryProposalUseCase(
                mock(GalleryProposalPort.class), users, TransactionRunner.direct(), CLOCK);
        assertThatThrownBy(() -> rejectProposal.execute(1L, 9L, "a".repeat(501)))
                .isInstanceOf(GalleryProposalApplicationException.class);

        var userBanCreate = new UserBanCreateUseCase(users, mock(UserBanRepositoryPort.class), CLOCK);
        assertThatThrownBy(() -> userBanCreate.execute(new UserBanCreateRequest(2L, 9L, 7, "a".repeat(501))))
                .isInstanceOf(UserApplicationException.class);

        GalleryAuthorityPort authPort = mock(GalleryAuthorityPort.class);
        GalleryManagementPort mgmtPort = mock(GalleryManagementPort.class);
        when(mgmtPort.findGalleryForUpdate(10L)).thenReturn(Optional.of(Gallery.create("재즈", "소개")));
        when(authPort.findActor(9L, 10L, CLOCK.instant().atOffset(ZoneOffset.UTC).toLocalDateTime()))
                .thenReturn(Optional.of(new GalleryActor(9L, UserRole.ADMIN, null, false)));
        var sanctionUseCase = new SanctionGalleryUserUseCase(
                authPort, mgmtPort, mock(GalleryBanPort.class), mock(GallerySubscriptionPort.class),
                users, mock(GalleryModerationLogPort.class), TransactionRunner.direct(), CLOCK);
        assertThatThrownBy(() -> sanctionUseCase.execute(9L, 10L, 2L, "SUSPEND", 3, "a".repeat(501)))
                .isInstanceOf(GalleryModerationApplicationException.class);

        // Search limits (100 code points)
        ProfileQueryPort profileQuery = mock(ProfileQueryPort.class);
        when(profileQuery.search("k".repeat(100), 0, 10)).thenReturn(new PageResult<>(List.of(), 0));
        var userSearch = new UserSearchUseCase(profileQuery, mock(FollowRepositoryPort.class), mock(MediaFileRepositoryPort.class));
        assertThat(userSearch.execute(new UserSearchRequest(1L, "k".repeat(100), 0, 10)).searchKeyword()).hasSize(100);
        assertThatThrownBy(() -> userSearch.execute(new UserSearchRequest(1L, "k".repeat(101), 0, 10)))
                .isInstanceOf(UserApplicationException.class);

        var postQuery = mock(PostQueryPort.class);
        var getPosts = new GetPostsUseCase(postQuery);
        assertThatThrownBy(() -> getPosts.execute(10L, 0, 20, "k".repeat(101), "LATEST"))
                .isInstanceOf(PostApplicationException.class);

        var getShorts = new GetShortsUseCase(mock(ShortsQueryPort.class), mock(ShortReactionCountCachePort.class));
        assertThat(getShorts.search("   ", null, 20, null)).isEqualTo(new ShortsPage(List.of(), null, false, 20));
        assertThatThrownBy(() -> getShorts.search("k".repeat(101), null, 20, null))
                .isInstanceOf(ShortsCompositionApplicationException.class);
    }

    @Test
    void textSanitizerStripsInvisibleAndControlCharsWhilePreservingHangulAndEmojis() {
        // Hangul fillers, Braille blank, zero-width & format chars, bidi controls, C0/DEL/C1 controls
        assertThat(TextSanitizer.sanitizeSingleLine("\u3164\u115F\u1160\uFFA0\u2800")).isEmpty();
        assertThat(TextSanitizer.sanitizeSingleLine("\u200B\u200C\u200E\u200F\u2060\uFEFF\u00AD")).isEmpty();
        assertThat(TextSanitizer.sanitizeSingleLine("\u202A\u202E\u2066\u2069\u0000\u0007\u007F\u0085")).isEmpty();
        assertThat(TextSanitizer.sanitizeSingleLine("\u00A0\u3000")).isEmpty();
        assertThat(TextSanitizer.sanitizeSingleLine("\u200D\uFE0F")).isEmpty();

        assertThat(TextSanitizer.hasVisibleContent("\u3164\u2800\u200B")).isFalse();
        assertThat(TextSanitizer.hasVisibleContent("가")).isTrue();

        // Single-line collapses newlines & Unicode spaces and strips invisible chars
        assertThat(TextSanitizer.sanitizeSingleLine("안\u3164녕\u00A0하\n세요\u200B"))
                .isEqualTo("안녕 하 세요");

        // Multi-line preserves internal \n while stripping invisible chars & normalizing spaces
        assertThat(TextSanitizer.sanitizeMultiline("\u200B첫\u3000줄\r\n둘\u3164째\u202E줄\n\u2800"))
                .isEqualTo("첫 줄\n둘째줄");

        // Positive Hangul + multi-code-point ZWJ emojis
        String family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
        String rainbowFlag = "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08";
        String heartOnFire = "\u2764\uFE0F\u200D\uD83D\uDD25";
        assertThat(TextSanitizer.sanitizeSingleLine("안녕하세요 " + family + " " + rainbowFlag + " " + heartOnFire))
                .isEqualTo("안녕하세요 " + family + " " + rainbowFlag + " " + heartOnFire);
    }
}

    @Test
    void textSanitizerHandlesLongMarkAndJoinerRunsWithoutRescanning() {
        String marks = "\u0301".repeat(100_000);
        String smile = "\uD83D\uDE00";
        String joiners = "\u200D".repeat(100_000);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertThat(TextSanitizer.sanitizeMultiline("a" + marks)).isEqualTo("a" + marks);
            assertThat(TextSanitizer.sanitizeMultiline(" " + marks)).isEmpty();
            assertThat(TextSanitizer.sanitizeSingleLine(smile + joiners + smile)).isEqualTo(smile + "\u200D" + smile);
            assertThat(TextSanitizer.sanitizeSingleLine(smile + "\uFE0F".repeat(50_000) + joiners + "x"))
                    .isEqualTo(smile + "\uFE0F".repeat(50_000) + "x");
        });
    }
