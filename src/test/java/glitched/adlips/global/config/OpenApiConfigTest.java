package glitched.adlips.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    private final OpenAPI openAPI = new OpenApiConfig().adlipsOpenAPI();

    @Test
    void describesAdlipsApi() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Narackrock API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
    }

    @Test
    void definesJwtBearerAuthentication() {
        var bearerAuth = openAPI.getComponents()
                .getSecuritySchemes()
                .get("bearerAuth");

        assertThat(bearerAuth.getType().toString()).isEqualTo("http");
        assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
        assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
    }
}
