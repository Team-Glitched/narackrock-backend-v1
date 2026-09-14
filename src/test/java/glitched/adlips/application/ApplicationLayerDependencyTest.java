package glitched.adlips.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApplicationLayerDependencyTest {

    @Test
    void application_layer에는_spring_의존성이_없다() throws IOException {
        Path applicationSource = Path.of("src/main/java/glitched/adlips/application");

        try (Stream<Path> paths = Files.walk(applicationSource)) {
            var springImports = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::springImports)
                    .toList();

            assertThat(springImports).isEmpty();
        }
    }

    private Stream<String> springImports(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> line.startsWith("import org.springframework."));
        } catch (IOException exception) {
            throw new IllegalStateException("애플리케이션 계층을 읽을 수 없습니다: " + path, exception);
        }
    }
}
