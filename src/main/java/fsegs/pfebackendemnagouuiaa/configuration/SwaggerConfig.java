package fsegs.pfebackendemnagouuiaa.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI 3 (Swagger UI) pour documenter et tester l'API REST.
 *
 * <p><b>Rôle :</b> déclarer le schéma de sécurité Bearer JWT afin que l'interface Swagger
 * propose le bouton « Authorize » et injecte {@code Authorization: Bearer &lt;token&gt;}.</p>
 *
 * <p><b>Relations :</b> les routes documentées sont exposées sous {@code /swagger-ui/**}
 * et {@code /v3/api-docs/**}, autorisées sans authentification dans
 * {@link SecurityConfig}.</p>
 */
@Configuration
public class SwaggerConfig {

    /**
     * Construit le modèle OpenAPI global avec exigence de sécurité Bearer par défaut.
     *
     * @return instance {@link OpenAPI} enrichie du schéma {@code bearerAuth}
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}
