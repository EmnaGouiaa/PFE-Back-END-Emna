package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.DemandeAuthentification;
import fsegs.pfebackendemnagouuiaa.dto.ReponseAuthentification;
import fsegs.pfebackendemnagouuiaa.services.ServiceAuthentification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Variante historique du contrôleur d'authentification (DTO et service en français).
 * <p>
 * <strong>Domaine exposé :</strong> connexion utilisateur (version {@code v1}).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/v1/authentification}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize} ; l'endpoint {@code /login}
 * est destiné à être public (configuration Spring Security globale).
 * <p>
 * <strong>Services injectés :</strong> {@link ServiceAuthentification}
 * <p>
 * <strong>Note :</strong> coexiste avec {@link AuthenticationController} sur {@code /api/auth}.
 * Préférer {@link AuthenticationController} pour les nouveaux clients front-end.
 */
@RestController
@RequestMapping("/api/v1/authentification")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class ControleurAuthentification {

    private final ServiceAuthentification serviceAuthentification;

    /**
     * Authentifie un utilisateur et retourne une réponse contenant le jeton d'accès.
     *
     * @param request demande d'authentification (identifiant, mot de passe)
     * @return {@link ResponseEntity} 200 avec {@link ReponseAuthentification}
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         en cas d'échec d'authentification
     */
    @PostMapping("/login")
    public ResponseEntity<ReponseAuthentification> login(@RequestBody DemandeAuthentification request) {
        // Délégation au service métier ; construction de la réponse HTTP 200
        return ResponseEntity.ok(serviceAuthentification.authentifier(request));
    }
}
