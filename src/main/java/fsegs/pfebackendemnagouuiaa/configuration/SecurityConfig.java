package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Configuration principale de Spring Security pour l'API REST stateless (JWT).
 *
 * <p><b>Rôle :</b> définir la chaîne de filtres, les règles d'autorisation par URL,
 * la politique CORS intégrée et les réponses JSON pour 401/403.</p>
 *
 * <p><b>Responsabilités :</b></p>
 * <ul>
 *   <li>Désactiver CSRF (API sans session cookie classique).</li>
 *   <li>Forcer {@link SessionCreationPolicy#STATELESS}.</li>
 *   <li>Autoriser les prérequêtes OPTIONS et les endpoints publics (auth, swagger, certaines listes).</li>
 *   <li>Insérer {@link JwtAuthenticationFilter} avant l'authentification formulaire.</li>
 *   <li>Activer {@code @PreAuthorize} via {@link EnableMethodSecurity}.</li>
 * </ul>
 *
 * <p><b>Relations :</b> s'appuie sur {@link ApplicationConfig#authenticationProvider()} et
 * {@link JwtAuthenticationFilter} ; complète {@link fsegs.pfebackendemnagouuiaa.exception.GlobalExceptionHandler}
 * pour le format des erreurs d'accès.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Construit la chaîne de filtres HTTP Security de l'application.
     *
     * @param http builder Spring Security
     * @return chaîne configurée (stateless + JWT + règles d'URL)
     * @throws Exception si la configuration est incohérente
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Prérequis CORS : autoriser OPTIONS sans authentification sur toutes les routes
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Routes publiques : authentification, documentation, catalogues ouverts
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/authentification/**",
                                "/api/stages/**",
                                "/api/offres/**",
                                "/api/offres-stage/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/stagiaires/**",
                                "/api/users",
                                "/api/entreprises/**"
                        ).permitAll()

                        .requestMatchers("/api/demandes-stage/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/reunions-finales/**").permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // Garantit que les 401/403 produits par Spring Security lui-meme
                        // retournent du JSON coherent avec le GlobalExceptionHandler,
                        // plutot que la reponse HTML/texte par defaut.
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            String msg = accessDeniedException.getMessage() != null
                                    ? accessDeniedException.getMessage()
                                    : "Acces refuse.";
                            response.getWriter().write("{\"message\":\"" + msg + "\"}");
                        })
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Source CORS utilisée par Spring Security ({@code cors(Customizer.withDefaults())}).
     *
     * @return configuration alignée sur le front Angular local
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Requested-With"
        ));
        configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
