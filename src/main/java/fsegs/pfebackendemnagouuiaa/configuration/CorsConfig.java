package fsegs.pfebackendemnagouuiaa.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration CORS dédiée via un filtre {@link CorsFilter} global.
 *
 * <p><b>Rôle :</b> autoriser le front Angular (port 4200) à appeler l'API avec cookies/credentials
 * et l'en-tête {@code Authorization} pour les JWT.</p>
 *
 * <p><b>Responsabilités :</b> définir origines, méthodes, en-têtes autorisés et durée de cache
 * des prérequêtes OPTIONS ({@code maxAge}).</p>
 *
 * <p><b>Relations :</b> complète (ou duplique partiellement) la source CORS de
 * {@link SecurityConfig#corsConfigurationSource()} ; en développement, les deux ciblent
 * {@code localhost:4200}.</p>
 *
 * <p><b>Note :</b> restreindre les origines en production aux domaines déployés.</p>
 */
@Configuration
public class CorsConfig  {

    /**
     * Enregistre un filtre CORS appliqué à toutes les routes {@code /**}.
     *
     * @return filtre Spring avec configuration Angular locale
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Autorise les cookies et l'en-tête Authorization côté navigateur
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Requested-With"
        ));
        config.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
