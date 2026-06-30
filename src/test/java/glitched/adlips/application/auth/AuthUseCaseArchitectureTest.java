package glitched.adlips.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AuthUseCaseArchitectureTest {

    @Test
    void authUseCasesDoNotDependOnSpringAnnotations() {
        Stream<Class<?>> useCases = Stream.of(GoogleLoginUseCase.class, SignUpWithGoogleUseCase.class);

        assertThat(useCases.flatMap(this::annotationsOnClassAndMethods)
                .map(annotation -> annotation.annotationType().getName()))
                .noneMatch(name -> name.startsWith("org.springframework."));
    }

    private Stream<Annotation> annotationsOnClassAndMethods(Class<?> type) {
        Stream<Annotation> classAnnotations = Arrays.stream(type.getAnnotations());
        Stream<Annotation> methodAnnotations = Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotations()));
        return Stream.concat(classAnnotations, methodAnnotations);
    }
}
