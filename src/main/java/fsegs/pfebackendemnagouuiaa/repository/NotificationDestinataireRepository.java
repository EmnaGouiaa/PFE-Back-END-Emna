package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.NotificationDestinataire;
import fsegs.pfebackendemnagouuiaa.entities.StatutActionNotification;
import fsegs.pfebackendemnagouuiaa.entities.StatutNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDestinataireRepository extends JpaRepository<NotificationDestinataire, Long> {
    boolean existsByNotificationIdAndUtilisateurId(Long notificationId, Long utilisateurId);

    List<NotificationDestinataire> findByUtilisateurIdOrderByNotificationDateCreationDesc(Long utilisateurId);

    List<NotificationDestinataire> findByUtilisateurId(Long utilisateurId);

    List<NotificationDestinataire> findByUtilisateurIdAndStatutNotification(Long utilisateurId, StatutNotification statutNotification);

    List<NotificationDestinataire> findByUtilisateurIdAndStatutActionNotif(Long utilisateurId, StatutActionNotification statutActionNotif);

    Optional<NotificationDestinataire> findByIdAndUtilisateurId(Long id, Long utilisateurId);

    long countByUtilisateurIdAndStatutNotification(Long utilisateurId, StatutNotification statutNotification);
}
