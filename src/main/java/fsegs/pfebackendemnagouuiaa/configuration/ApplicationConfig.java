package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import jakarta.servlet.MultipartConfigElement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration Spring des beans transverses : sécurité, HTTP client et uploads.
 *
 * <p><b>Rôle :</b> centraliser l'infrastructure utilisée par l'authentification JWT,
 * les appels REST sortants et la gestion des pièces jointes volumineuses.</p>
 *
 * <p><b>Beans fournis :</b></p>
 * <ul>
 *   <li>{@link RestTemplate} — intégrations HTTP synchrones.</li>
 *   <li>{@link UserDetailsService} — chargement utilisateur par e-mail (insensible à la casse).</li>
 *   <li>{@link AuthenticationProvider} — DAO + BCrypt.</li>
 *   <li>{@link AuthenticationManager} — point d'entrée login.</li>
 *   <li>{@link MultipartConfigElement} — limite 100 Mo par fichier/requête.</li>
 * </ul>
 *
 * <p><b>Relations :</b> consommé par {@link SecurityConfig} et {@link JwtAuthenticationFilter} ;
 * s'appuie sur {@link UtilisateurRepository}.</p>
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UtilisateurRepository utilisateurRepository;


    /**
     * Client HTTP synchrone pour appels vers API externes (Trello, etc.).
     *
     * @return instance par défaut {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Charge un utilisateur Spring Security à partir de son e-mail (identifiant de connexion).
     *
     * @return lambda résolvant {@link fsegs.pfebackendemnagouuiaa.entities.Utilisateur}
     * @throws UsernameNotFoundException si l'e-mail est inconnu
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> utilisateurRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    }

    /**
     * Fournit le provider d'authentification DAO avec encodeur BCrypt.
     *
     * @return {@link DaoAuthenticationProvider} configuré avec {@link #userDetailsService()}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Encodeur de mots de passe BCrypt (facteur par défaut Spring Security).
     *
     * @return {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Délègue la création du gestionnaire d'authentification à Spring Security.
     *
     * @param config configuration d'authentification Spring
     * @return {@link AuthenticationManager}
     * @throws Exception si la configuration est invalide
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configure les limites d'upload multipart (CV, conventions, rapports).
     *
     * @return configuration servlet avec plafond 100 Mo
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(100));
        factory.setMaxRequestSize(DataSize.ofMegabytes(100));
        return factory.createMultipartConfig();
    }
}
