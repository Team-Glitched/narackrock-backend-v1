package glitched.adlips.application.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 갤러리·어드민 관리 유스케이스마다 권한 거부 테스트가 있는지 확인한다.
 * 단순 문자열 검사라 완전한 보장은 아니며, docs/community-moderation/guardrails.md 체크리스트와 함께 쓴다.
 */
class ModerationAuthorizationCoverageTest {

    private static final Path MAIN = Path.of("src/main/java/glitched/adlips/application/community");
    private static final Path TEST = Path.of("src/test/java/glitched/adlips/application/community");
    private static final Pattern MODERATION_USE_CASE = Pattern.compile(
            "(Approve\\w*|Reject\\w*Proposal\\w*|GetGalleryProposals|Sanction\\w*|Lift\\w*Ban\\w*"
                    + "|Moderate\\w*|UpdateGallery|GetGalleryBans|GetGalleryModerationLogs"
                    + "|Grant\\w*Manager|Revoke\\w*Manager|GetGalleryManagers)UseCase\\.java");

    @Test
    void 관리_유스케이스마다_권한_거부_테스트가_있다() throws IOException {
        List<String> missing;
        try (Stream<Path> files = Files.list(MAIN)) {
            missing = files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> MODERATION_USE_CASE.matcher(name).matches())
                    .filter(name -> !hasForbiddenCase(name.replace(".java", "Test.java")))
                    .toList();
        }

        assertThat(missing).as("FORBIDDEN 케이스가 없는 관리 유스케이스 테스트").isEmpty();
    }

    private static boolean hasForbiddenCase(String testFileName) {
        Path testFile = TEST.resolve(testFileName);
        try {
            return Files.exists(testFile) && Files.readString(testFile).contains("FORBIDDEN");
        } catch (IOException e) {
            return false;
        }
    }
}
