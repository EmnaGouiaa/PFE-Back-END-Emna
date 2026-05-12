package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStageId(Long stageId);

    List<Notification> findByReunionId(Long reunionId);

    List<Notification> findByReunionFinaleId(Long reunionFinaleId);

    List<Notification> findByReunionHebdomadaireId(Long reunionHebdomadaireId);

    List<Notification> findByCahierStageId(Long cahierStageId);

    List<Notification> findByFicheEvaluationId(Long ficheEvaluationId);

    List<Notification> findByConventionStageId(Long conventionId);

    List<Notification> findByDemandeCreationCompteEntrepriseId(Long demandeId);
}
