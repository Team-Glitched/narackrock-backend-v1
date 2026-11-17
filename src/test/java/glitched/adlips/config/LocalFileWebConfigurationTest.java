package glitched.adlips.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.auth.token-secret=test-only-secret-value-32-characters",
        "spring.datasource.url=jdbc:h2:mem:files-cache;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LocalFileWebConfigurationTest {
    @TempDir
    static Path root;

    @DynamicPropertySource
    static void storageRoot(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-root", root::toString);
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void storedFilesAreCacheableThroughTheSecurityChain() throws Exception {
        Path file = root.resolve("exports/8/20/mix.wav");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1, 2, 3});

        mvc.perform(get("/files/exports/8/20/mix.wav"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3}))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(header().doesNotExist("Pragma"))
                .andExpect(header().doesNotExist("Expires"));
    }

    @Test
    void profileImagesAreCacheable() throws Exception {
        Path file = root.resolve("profiles/1/avatar.png");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1});

        mvc.perform(get("/files/profiles/1/avatar.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void uploadsAreNotCachedWhenTheirContentsChange() throws Exception {
        Path file = root.resolve("audio/1/upload.wav");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1});

        mvc.perform(get("/files/audio/1/upload.wav"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1}))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));

        Files.write(file, new byte[] {2});
        mvc.perform(get("/files/audio/1/upload.wav"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {2}))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
    }

    @Test
    void missingFilesAreNotCached() throws Exception {
        mvc.perform(get("/files/exports/8/21/mix.wav"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
    }
}
