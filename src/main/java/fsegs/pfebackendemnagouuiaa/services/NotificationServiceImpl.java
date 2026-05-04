package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.NotificationDto;
import fsegs.pfebackendemnagouuiaa.entities.Notification;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.NotificationRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String TYPE_DEMANDE_ENTREPRISE_VALIDEE = "DEMANDE_ENTREPRISE_VALIDEE";
    private static final String TYPE_STAGE_AFFECTE = "STAGE_AFFECTE";
    private static final String TYPE_REUNION_FIXEE = "REUNION_FIXEE";

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    public void notifierStagiaireReunionFixee(Long stagiaireId, Long reunionId, String message) {
        log.info("Notification reunion enregistree pour stagiaireId={} : {}", stagiaireId, message);
        if (stagiaireId == null) {
            return;
        }

        creerNotification(
                stagiaireId,
                "Nouvelle reunion planifiee",
                message,
                TYPE_REUNION_FIXEE,
                reunionId,
                "REUNION"
        );
    }

    @Override
    public void notifierCreationCompteEntreprise(String email, String motDePasseTemporaire, String nomEntreprise) {
        log.info(
                "Creation compte entreprise - Email: {} - Entreprise: {} - Mot de passe temporaire: {}",
                email,
                nomEntreprise,
                motDePasseTemporaire
        );
    }

    @Override
    @Transactional
    public NotificationDto creerNotification(Long userId,
                                             String title,
                                             String message,
                                             String type,
                                             Long relatedEntityId,
                                             String relatedEntityType) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        Notification notification = Notification.builder()
                .utilisateur(utilisateur)
                .titre(title)
                .message(message)
                .creeLe(LocalDateTime.now())
                .lu(false)
                .type(type)
                .entiteId(relatedEntityId)
                .entiteType(relatedEntityType)
                .build();

        return toDto(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUtilisateurIdOrderByCreeLeDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByUserId(Long userId) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(userId);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification introuvable"));

        Long ownerId = notification.getUtilisateur() != null ? notification.getUtilisateur().getId() : null;
        if (!userId.equals(ownerId)) {
            throw new EntityNotFoundException("Notification introuvable pour cet utilisateur");
        }

        if (!Boolean.TRUE.equals(notification.getLu())) {
            notification.setLu(true);
            notification.setLuLe(LocalDateTime.now());
        }

        return toDto(notificationRepository.save(notification));
    }

    @Override
    public void notifierDemandeEntrepriseValidee(Long stagiaireId, Long demandeEntrepriseId, String nomEntreprise) {
        creerNotification(
                stagiaireId,
                "Demande entreprise approuvee",
                "Votre demande de creation du compte entreprise " + safeName(nomEntreprise) + " a ete approuvee par les deux acteurs.",
                TYPE_DEMANDE_ENTREPRISE_VALIDEE,
                demandeEntrepriseId,
                "DEMANDE_ENTREPRISE"
        );
    }

    @Override
    public void notifierStageAffecte(Long stagiaireId, Long stageId, String titreStage, String nomEntreprise) {
        creerNotification(
                stagiaireId,
                "Stage affecte",
                "Vous avez ete affecte au stage " + safeName(titreStage) + " chez " + safeName(nomEntreprise) + ".",
                TYPE_STAGE_AFFECTE,
                stageId,
                "STAGE"
        );
    }

    private NotificationDto toDto(Notification notification) {
        Long userId = notification.getUtilisateur() != null ? notification.getUtilisateur().getId() : null;
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(userId)
                .title(notification.getTitre())
                .message(notification.getMessage())
                .dateTime(notification.getCreeLe())
                .read(Boolean.TRUE.equals(notification.getLu()))
                .readAt(notification.getLuLe())
                .type(notification.getType())
                .relatedEntityId(notification.getEntiteId())
                .relatedEntityType(notification.getEntiteType())
                .build();
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "selectionne";
        }
        return value.trim();
    }
}
