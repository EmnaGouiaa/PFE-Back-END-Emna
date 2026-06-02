package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.MailTestRequest;
import fsegs.pfebackendemnagouuiaa.dto.MailTestResponse;
import fsegs.pfebackendemnagouuiaa.services.AccountEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST d'administration pour tester la configuration SMTP.
 * <p>
 * <strong>Domaine exposé :</strong> envoi d'e-mails de test (diagnostic infrastructure).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/admin/mail}
 * <p>
 * <strong>Sécurité :</strong> {@code @PreAuthorize("hasRole('ADMINISTRATEUR')")} au niveau classe.
 * <p>
 * <strong>Services injectés :</strong> {@link AccountEmailService}
 */
@RestController
@RequestMapping("/api/admin/mail")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminMailController {

    private final AccountEmailService accountEmailService;

    /**
     * Envoie un e-mail de test à l'adresse indiquée pour valider la configuration SMTP.
     *
     * @param request destinataire du message de test
     * @return {@link ResponseEntity} 200 avec {@link MailTestResponse} (succès ou détail d'erreur)
     * @throws jakarta.validation.ConstraintViolationException si l'e-mail est invalide
     */
    @PostMapping("/test")
    public ResponseEntity<MailTestResponse> sendTestEmail(@Valid @RequestBody MailTestRequest request) {
        // Rôle ADMINISTRATEUR imposé par @PreAuthorize sur la classe
        return ResponseEntity.ok(accountEmailService.testSmtpAndSend(request.getRecipientEmail()));
    }
}
