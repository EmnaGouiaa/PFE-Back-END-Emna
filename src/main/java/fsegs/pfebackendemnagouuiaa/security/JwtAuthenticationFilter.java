package fsegs.pfebackendemnagouuiaa.security;

import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre servlet exécuté une fois par requête pour authentifier via JWT.
 *
 * <p><b>Rôle :</b> intercepter l'en-tête {@code Authorization: Bearer &lt;token&gt;},
 * valider le jeton et peupler le {@link SecurityContextHolder} avant les contrôleurs.</p>
 *
 * <p><b>Chaîne de traitement :</b></p>
 * <ol>
 *   <li>Absence ou format incorrect du header → laisser passer (routes publiques).</li>
 *   <li>Extraction du sujet (e-mail) via {@link JwtService}.</li>
 *   <li>Vérification du compte actif en base ({@link Utilisateur#getActif()}).</li>
 *   <li>Chargement {@link UserDetails} et validation signature/expiration.</li>
 *   <li>Installation du token d'authentification Spring Security.</li>
 * </ol>
 *
 * <p><b>Relations :</b> enregistré avant {@code UsernamePasswordAuthenticationFilter}
 * dans {@link fsegs.pfebackendemnagouuiaa.configuration.SecurityConfig} ;
 * complète {@link fsegs.pfebackendemnagouuiaa.configuration.ApplicationConfig#userDetailsService()}.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Point d'entrée du filtre : authentification JWT ou poursuite anonyme.
     *
     * @param request     requête HTTP entrante
     * @param response    réponse HTTP (peut recevoir 403 JSON)
     * @param filterChain chaîne de filtres restants
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'écriture de la réponse
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException,
            IOException {
        final String authHeader = request.getHeader("Authorization");
        final String userEmail;

        // Pas de jeton : requête anonyme ou route publique — ne pas bloquer ici
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraction du JWT après le préfixe "Bearer " (7 caractères)
        final String jwt= authHeader.substring(7);
        userEmail =jwtService.extractUsername(jwt);
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(userEmail).orElse(null);
            // Use Boolean.FALSE.equals so that actif=null (old/migrated accounts) is treated as
            // active, matching the behaviour of Utilisateur.isEnabled().
            // !Boolean.TRUE.equals(null) == true would wrongly block those accounts.
            // Règle sécurité : seul actif=false explicite bloque la requête
            if (utilisateur != null && Boolean.FALSE.equals(utilisateur.getActif())) {
                writeForbiddenResponse(response, "Compte desactive.");
                return;
            }

            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                // Validation cryptographique et temporelle du token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (UsernameNotFoundException ex) {
                writeForbiddenResponse(response, "Utilisateur introuvable ou inactif.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Écrit une réponse HTTP 403 au format JSON minimal, alignée sur le GlobalExceptionHandler.
     *
     * @param response flux de réponse HTTP
     * @param message  texte du champ {@code message}
     * @throws IOException si l'écriture du corps échoue
     */
    private void writeForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
