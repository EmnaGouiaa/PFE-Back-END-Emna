package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateNotificationRequest;
import fsegs.pfebackendemnagouuiaa.dto.NotificationDestinataireResponse;
import fsegs.pfebackendemnagouuiaa.dto.NotificationDto;
import fsegs.pfebackendemnagouuiaa.entities.TypeNotification;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;

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

    List<NotificationDestinataireResponse> createNotification(String titre,
                                                              String body,
                                                              TypeNotification typeNotification,
                                                              List<Utilisateur> destinataires);

    List<NotificationDestinataireResponse> createNotification(CreateNotificationRequest request);

    List<NotificationDestinataireResponse> getNotificationsByUtilisateur(Long utilisateurId);

    long countUnreadByUserId(Long userId);

    NotificationDestinataireResponse markAsRead(Long notificationDestinataireId);

    int markAllAsRead(Long utilisateurId);

    NotificationDestinataireResponse markActionDone(Long notificationDestinataireId, String reponse);

    NotificationDestinataireResponse cancelNotificationForUser(Long notificationDestinataireId);

    boolean isOwnedByUtilisateur(Long notificationDestinataireId, Long utilisateurId);

    void notifierDemandeEntrepriseValidee(Long stagiaireId, Long demandeEntrepriseId, String nomEntreprise);

    void notifierDemandeEntrepriseRefusee(Long stagiaireId, Long demandeEntrepriseId, String motifRefus);

    void notifierStageAffecte(Long stagiaireId, Long stageId, String titreStage, String nomEntreprise);
}
