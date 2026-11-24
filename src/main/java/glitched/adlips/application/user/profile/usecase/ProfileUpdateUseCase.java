package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.user.profile.dto.request.UserProfileUpdateRequest;
import glitched.adlips.application.user.profile.dto.response.UserProfileUpdateResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.common.TextSanitizer;
import glitched.adlips.domain.user.Profile;
import java.time.Clock;
import java.time.LocalDateTime;
public class ProfileUpdateUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public ProfileUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            Clock clock
    ) {
        this(userRepository, profileRepository, clock, TransactionRunner.direct());
    }

    public ProfileUpdateUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    public UserProfileUpdateResponse execute(UserProfileUpdateRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private UserProfileUpdateResponse executeInternal(UserProfileUpdateRequest request) {
        if (request == null) {
            throw validation("수정할 프로필 정보를 입력해 주세요.");
        }
        Long userId = request.userId();
        String nickname = normalizeNickname(request.nickname());
        String instrument = normalizeOptional(request.primaryInstrument(), false);
        String explanation = normalizeOptional(request.explanation(), true);
        validateOptionalLength(instrument, ContentLimits.INSTRUMENT_MAX_LENGTH,
                "주 악기는 " + ContentLimits.INSTRUMENT_MAX_LENGTH + "자 이내로 입력해 주세요.");
        validateOptionalLength(explanation, ContentLimits.PROFILE_BIO_MAX_LENGTH,
                "자기소개는 " + ContentLimits.PROFILE_BIO_MAX_LENGTH + "자 이내로 입력해 주세요.");
        requireActiveUser(userId);
        Profile profile = requireProfile(userId);
        boolean nicknameChanged = nickname != null && !nickname.equals(profile.getNickname());
        if (nicknameChanged && profileRepository.existsByNicknameAndUserIdNot(nickname, userId)) {
            throw new UserApplicationException(UserErrorCode.DUPLICATE_NICKNAME, "이미 사용중인 닉네임입니다.");
        }
        Profile saved = profileRepository.save(profile.update(
                nickname,
                instrument,
                explanation,
                LocalDateTime.now(clock)
        ));
        return new UserProfileUpdateResponse(
                saved.getNickname(), saved.getPrimaryInstrument(), saved.getExplanation()
        );
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        String trimmed = TextSanitizer.sanitizeSingleLine(nickname);
        if (trimmed.isEmpty()) {
            throw validation("닉네임이 입력되지 않았습니다.");
        }
        if (ContentLimits.exceeds(trimmed, ContentLimits.NICKNAME_MAX_LENGTH)) {
            throw validation("닉네임은 " + ContentLimits.NICKNAME_MAX_LENGTH + "자 이내로 입력해 주세요.");
        }
        return trimmed;
    }

    private String normalizeOptional(String value, boolean multiline) {
        if (value == null) {
            return null;
        }
        return multiline ? TextSanitizer.sanitizeMultiline(value) : TextSanitizer.sanitizeSingleLine(value);
    }

    private void validateOptionalLength(String value, int maxLength, String message) {
        if (value != null && ContentLimits.exceeds(value, maxLength)) {
            throw validation(message);
        }
    }

    private void requireActiveUser(Long userId) {
        userRepository.findById(userId)
                .filter(user -> user.isActive())
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
    }

    private Profile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.PROFILE_NOT_FOUND,
                        "존재하지 않는 사용자 프로필입니다."
                ));
    }

    private UserApplicationException validation(String message) {
        return new UserApplicationException(UserErrorCode.VALIDATION_ERROR, message);
    }
}
