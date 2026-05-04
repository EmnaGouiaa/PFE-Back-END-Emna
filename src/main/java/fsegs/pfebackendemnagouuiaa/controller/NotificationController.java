package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.NotificationDto;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.services.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @GetMapping("/utilisateur/{userId}")
    public List<NotificationDto> getNotificationsByUserId(@PathVariable Long userId) {
        ensureCanAccessUserNotifications(userId);
        return notificationService.getNotificationsByUserId(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/utilisateur/{userId}/non-lues/count")
    public Map<String, Long> countUnreadByUserId(@PathVariable Long userId) {
        ensureCanAccessUserNotifications(userId);
        return Map.of("count", notificationService.countUnreadByUserId(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{notificationId}/lu")
    public NotificationDto markAsRead(@PathVariable Long notificationId, @RequestParam Long userId) {
        ensureCanAccessUserNotifications(userId);
        return notificationService.markAsRead(notificationId, userId);
    }

    private void ensureCanAccessUserNotifications(Long userId) {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur authentifie introuvable"));

        if (authenticatedUser.getRole() == Role.ADMINISTRATEUR || authenticatedUser.getId().equals(userId)) {
            return;
        }

        throw new AccessDeniedException("Acces refuse aux notifications de cet utilisateur.");
    }
}
