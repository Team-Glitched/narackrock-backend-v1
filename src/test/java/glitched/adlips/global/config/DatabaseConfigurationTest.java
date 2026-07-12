package glitched.adlips.global.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {

    @Test
    void productionDatabaseDialectIsConfiguredForPostgreSQL() throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(inputStream);
        }

        assertThat(properties.getProperty("spring.jpa.database-platform"))
                .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    }
}
