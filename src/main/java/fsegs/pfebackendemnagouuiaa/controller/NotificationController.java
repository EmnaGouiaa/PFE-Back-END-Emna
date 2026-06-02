package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CreateNotificationRequest;
import fsegs.pfebackendemnagouuiaa.dto.NotificationActionRequest;
import fsegs.pfebackendemnagouuiaa.dto.NotificationDestinataireResponse;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.services.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST des notifications in-app (destinataires, lecture, actions).
 * <p>
 * <strong>Domaine exposé :</strong> notifications utilisateur, compteurs non lues, actions (lu, fait, annulé).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/notifications}
 * <p>
 * <strong>Sécurité :</strong> {@code isAuthenticated()} sur les endpoints publics ;
 * contrôle d'accès applicatif via {@link #ensureCanAccessUserNotifications(Long)} et
 * {@link #ensureCanAccessNotificationDestinataire(Long)} (propriétaire ou administrateur).
 * <p>
 * <strong>Services injectés :</strong>
 * <ul>
 *   <li>{@link NotificationService} — persistance et règles métier</li>
 *   <li>{@link JwtService} — résolution de l'utilisateur connecté</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    /**
     * Crée une notification et ses entrées destinataires.
     *
     * @param request contenu, type, liste de destinataires
     * @return liste des {@link NotificationDestinataireResponse} créées
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public List<NotificationDestinataireResponse> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.createNotification(request);
    }

    /**
     * Liste les notifications d'un utilisateur.
     *
     * @param utilisateurId identifiant du destinataire
     * @return liste des notifications pour cet utilisateur
     * @throws AccessDeniedException si l'appelant n'est ni admin ni le propriétaire
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/utilisateur/{utilisateurId}")
    public List<NotificationDestinataireResponse> getNotificationsByUtilisateur(@PathVariable Long utilisateurId) {
        ensureCanAccessUserNotifications(utilisateurId);
        return notificationService.getNotificationsByUtilisateur(utilisateurId);
    }

    /**
     * Compte les notifications non lues d'un utilisateur.
     *
     * @param utilisateurId identifiant du destinataire
     * @return map {@code {"count": n}}
     * @throws AccessDeniedException si accès refusé
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/utilisateur/{utilisateurId}/non-lues/count")
    public Map<String, Long> countUnreadByUserId(@PathVariable Long utilisateurId) {
        ensureCanAccessUserNotifications(utilisateurId);
        return Map.of("count", notificationService.countUnreadByUserId(utilisateurId));
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     *
     * @param userId identifiant du destinataire
     * @return map {@code {"markedAsRead": n}} avec le nombre d'entrées mises à jour
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/utilisateur/{userId}/read-all")
    public Map<String, Object> markAllAsRead(@PathVariable Long userId) {
        ensureCanAccessUserNotifications(userId);
        int count = notificationService.markAllAsRead(userId);
        return Map.of("markedAsRead", count);
    }

    /**
     * Marque une notification destinataire comme lue.
     *
     * @param id identifiant de l'entrée destinataire
     * @return {@link NotificationDestinataireResponse} mise à jour
     * @throws AccessDeniedException si l'entrée n'appartient pas à l'utilisateur connecté
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/destinataire/{id}/read")
    public NotificationDestinataireResponse markAsRead(@PathVariable Long id) {
        ensureCanAccessNotificationDestinataire(id);
        return notificationService.markAsRead(id);
    }

    /**
     * Marque une action de notification comme effectuée (avec réponse optionnelle).
     *
     * @param id      identifiant de l'entrée destinataire
     * @param request réponse utilisateur (optionnelle)
     * @return {@link NotificationDestinataireResponse} mise à jour
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/destinataire/{id}/done")
    public NotificationDestinataireResponse markActionDone(@PathVariable Long id,
                                                           @RequestBody(required = false) NotificationActionRequest request) {
        ensureCanAccessNotificationDestinataire(id);
        return notificationService.markActionDone(id, request != null ? request.getReponse() : null);
    }

    /**
     * Annule une notification pour l'utilisateur destinataire.
     *
     * @param id identifiant de l'entrée destinataire
     * @return {@link NotificationDestinataireResponse} annulée
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/destinataire/{id}/cancel")
    public NotificationDestinataireResponse cancelNotificationForUser(@PathVariable Long id) {
        ensureCanAccessNotificationDestinataire(id);
        return notificationService.cancelNotificationForUser(id);
    }

    /**
     * Vérifie que l'utilisateur connecté peut consulter les notifications du {@code userId} cible.
     * <p>Règle : administrateur ou propriétaire du compte uniquement.
     */
    private void ensureCanAccessUserNotifications(Long userId) {
        Utilisateur authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser.getRole() == Role.ADMINISTRATEUR || authenticatedUser.getId().equals(userId)) {
            return;
        }
        throw new AccessDeniedException("Acces refuse aux notifications de cet utilisateur.");
    }

    /**
     * Vérifie que l'utilisateur connecté est propriétaire de l'entrée destinataire ou administrateur.
     */
    private void ensureCanAccessNotificationDestinataire(Long notificationDestinataireId) {
        Utilisateur authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser.getRole() == Role.ADMINISTRATEUR
                || notificationService.isOwnedByUtilisateur(notificationDestinataireId, authenticatedUser.getId())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse a cette notification.");
    }

    /** Résout l'utilisateur à partir du contexte JWT. */
    private Utilisateur getAuthenticatedUser() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur authentifie introuvable"));
    }
}
