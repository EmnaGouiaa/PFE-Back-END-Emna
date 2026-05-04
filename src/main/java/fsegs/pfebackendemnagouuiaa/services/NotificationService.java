package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.NotificationDto;

import java.util.List;

public interface NotificationService {
    void notifierStagiaireReunionFixee(Long stagiaireId, Long reunionId, String message);

    void notifierCreationCompteEntreprise(String email, String motDePasseTemporaire, String nomEntreprise);

    NotificationDto creerNotification(Long userId,
                                       String title,
                                       String message,
                                       String type,
                                       Long relatedEntityId,
                                       String relatedEntityType);

    List<NotificationDto> getNotificationsByUserId(Long userId);

    long countUnreadByUserId(Long userId);

    NotificationDto markAsRead(Long notificationId, Long userId);

    void notifierDemandeEntrepriseValidee(Long stagiaireId, Long demandeEntrepriseId, String nomEntreprise);

    void notifierStageAffecte(Long stagiaireId, Long stageId, String titreStage, String nomEntreprise);
}
