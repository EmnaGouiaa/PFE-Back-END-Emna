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

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public List<NotificationDestinataireResponse> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.createNotification(request);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/utilisateur/{utilisateurId}")
    public List<NotificationDestinataireResponse> getNotificationsByUtilisateur(@PathVariable Long utilisateurId) {
        ensureCanAccessUserNotifications(utilisateurId);
        return notificationService.getNotificationsByUtilisateur(utilisateurId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/utilisateur/{utilisateurId}/non-lues/count")
    public Map<String, Long> countUnreadByUserId(@PathVariable Long utilisateurId) {
        ensureCanAccessUserNotifications(utilisateurId);
        return Map.of("count", notificationService.countUnreadByUserId(utilisateurId));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/utilisateur/{userId}/read-all")
    public Map<String, Object> markAllAsRead(@PathVariable Long userId) {
        ensureCanAccessUserNotifications(userId);
        int count = notificationService.markAllAsRead(userId);
        return Map.of("markedAsRead", count);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/destinataire/{id}/read")
    public NotificationDestinataireResponse markAsRead(@PathVariable Long id) {
        ensureCanAccessNotificationDestinataire(id);
        return notificationService.markAsRead(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/destinataire/{id}/done")
    public NotificationDestinataireResponse markActionDone(@PathVariable Long id,
                                                           @RequestBody(required = false) NotificationActionRequest request) {
        ensureCanAccessNotificationDestinataire(id);
        return notificationService.markActionDone(id, request != null ? request.getReponse() : null);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/destinataire/{id}/cancel")
    public NotificationDestinataireResponse cancelNotificationForUser(@PathVariable Long id) {
        ensureCanAccessNotificationDestinataire(id);
        return notificationService.cancelNotificationForUser(id);
    }

    private void ensureCanAccessUserNotifications(Long userId) {
        Utilisateur authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser.getRole() == Role.ADMINISTRATEUR || authenticatedUser.getId().equals(userId)) {
            return;
        }
        throw new AccessDeniedException("Acces refuse aux notifications de cet utilisateur.");
    }

    private void ensureCanAccessNotificationDestinataire(Long notificationDestinataireId) {
        Utilisateur authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser.getRole() == Role.ADMINISTRATEUR
                || notificationService.isOwnedByUtilisateur(notificationDestinataireId, authenticatedUser.getId())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse a cette notification.");
    }

    private Utilisateur getAuthenticatedUser() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur authentifie introuvable"));
    }
}
