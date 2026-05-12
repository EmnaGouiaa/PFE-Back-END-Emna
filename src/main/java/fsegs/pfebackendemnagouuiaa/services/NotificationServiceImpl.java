package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateNotificationRequest;
import fsegs.pfebackendemnagouuiaa.dto.NotificationDestinataireResponse;
import fsegs.pfebackendemnagouuiaa.dto.NotificationDto;
import fsegs.pfebackendemnagouuiaa.entities.Notification;
import fsegs.pfebackendemnagouuiaa.entities.NotificationDestinataire;
import fsegs.pfebackendemnagouuiaa.entities.StatutActionNotification;
import fsegs.pfebackendemnagouuiaa.entities.StatutNotification;
import fsegs.pfebackendemnagouuiaa.entities.TypeNotification;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.DemandeCreationCompteEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.NotificationDestinataireRepository;
import fsegs.pfebackendemnagouuiaa.repository.NotificationRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionHebdomadaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String TYPE_DEMANDE_ENTREPRISE_VALIDEE = "DEMANDE_ENTREPRISE_VALIDEE";
    private static final String TYPE_DEMANDE_ENTREPRISE_REFUSEE = "DEMANDE_ENTREPRISE_REFUSEE";
    private static final String TYPE_STAGE_AFFECTE = "STAGE_AFFECTE";
    private static final String TYPE_REUNION_FIXEE = "REUNION_FIXEE";

    private final NotificationRepository notificationRepository;
    private final NotificationDestinataireRepository notificationDestinataireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StageRepository stageRepository;
    private final ReunionRepository reunionRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final ReunionHebdomadaireRepository reunionHebdomadaireRepository;
    private final CahierStageRepository cahierStageRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final ConventionStageRepository conventionStageRepository;
    private final DemandeCreationCompteEntrepriseRepository demandeCreationCompteEntrepriseRepository;

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
                .titre(title)
                .body(message)
                .dateCreation(LocalDateTime.now())
                .typeNotification(TypeNotification.fromValue(type))
                .build();

        applyBusinessContext(notification, relatedEntityId, relatedEntityType);

        Notification savedNotification = notificationRepository.save(notification);

        NotificationDestinataire notificationDestinataire = NotificationDestinataire.builder()
                .notification(savedNotification)
                .utilisateur(utilisateur)
                .statutNotification(StatutNotification.NON_LUE)
                .statutActionNotif(StatutActionNotification.PENDING)
                .build();

        NotificationDestinataireResponse response = toResponse(
                notificationDestinataireRepository.save(notificationDestinataire)
        );

        return NotificationDto.builder()
                .id(response.getNotificationId())
                .userId(userId)
                .title(response.getTitre())
                .message(response.getBody())
                .dateTime(response.getDateCreation())
                .read(response.getStatutNotification() == StatutNotification.LUE)
                .readAt(response.getDateLecture())
                .type(response.getTypeNotification().name())
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();
    }

    @Override
    @Transactional
    public List<NotificationDestinataireResponse> createNotification(String titre,
                                                                     String body,
                                                                     TypeNotification typeNotification,
                                                                     List<Utilisateur> destinataires) {
        if (destinataires == null || destinataires.isEmpty()) {
            throw new IllegalArgumentException("Au moins un destinataire est obligatoire.");
        }

        Set<Long> uniqueDestinataireIds = destinataires.stream()
                .filter(Objects::nonNull)
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (uniqueDestinataireIds.isEmpty()) {
            throw new IllegalArgumentException("Aucun destinataire valide fourni.");
        }

        List<Utilisateur> utilisateurs = utilisateurRepository.findAllById(uniqueDestinataireIds);
        if (utilisateurs.size() != uniqueDestinataireIds.size()) {
            throw new EntityNotFoundException("Un ou plusieurs destinataires sont introuvables.");
        }

        Notification notification = Notification.builder()
                .titre(titre)
                .body(body)
                .dateCreation(LocalDateTime.now())
                .typeNotification(typeNotification)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        List<NotificationDestinataire> destinatairesToPersist = utilisateurs.stream()
                .filter(utilisateur -> !notificationDestinataireRepository.existsByNotificationIdAndUtilisateurId(
                        savedNotification.getId(),
                        utilisateur.getId()
                ))
                .map(utilisateur -> NotificationDestinataire.builder()
                        .notification(savedNotification)
                        .utilisateur(utilisateur)
                        .statutNotification(StatutNotification.NON_LUE)
                        .statutActionNotif(StatutActionNotification.PENDING)
                        .build())
                .toList();

        return notificationDestinataireRepository.saveAll(destinatairesToPersist)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<NotificationDestinataireResponse> createNotification(CreateNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requete de creation est obligatoire.");
        }

        Set<Long> uniqueDestinataireIds = request.getDestinataireIds() == null
                ? Set.of()
                : new LinkedHashSet<>(request.getDestinataireIds());

        List<Utilisateur> destinataires = utilisateurRepository.findAllById(uniqueDestinataireIds);
        if (destinataires.size() != uniqueDestinataireIds.size()) {
            throw new EntityNotFoundException("Un ou plusieurs destinataires sont introuvables.");
        }

        Notification parentNotification = null;
        if (request.getParentNotificationId() != null) {
            parentNotification = notificationRepository.findById(request.getParentNotificationId())
                    .orElseThrow(() -> new EntityNotFoundException("Notification parente introuvable."));
        }

        Notification notification = Notification.builder()
                .titre(request.getTitre())
                .body(request.getBody())
                .dateCreation(LocalDateTime.now())
                .typeNotification(request.getTypeNotification())
                .parentNotification(parentNotification)
                .build();

        applyBusinessContext(notification, request);

        Notification savedNotification = notificationRepository.save(notification);

        List<NotificationDestinataire> destinatairesToPersist = destinataires.stream()
                .filter(utilisateur -> !notificationDestinataireRepository.existsByNotificationIdAndUtilisateurId(
                        savedNotification.getId(),
                        utilisateur.getId()
                ))
                .map(utilisateur -> NotificationDestinataire.builder()
                        .notification(savedNotification)
                        .utilisateur(utilisateur)
                        .statutNotification(StatutNotification.NON_LUE)
                        .statutActionNotif(StatutActionNotification.PENDING)
                        .build())
                .toList();

        return notificationDestinataireRepository.saveAll(destinatairesToPersist)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDestinataireResponse> getNotificationsByUtilisateur(Long utilisateurId) {
        return notificationDestinataireRepository.findByUtilisateurIdOrderByNotificationDateCreationDesc(utilisateurId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByUserId(Long userId) {
        return notificationDestinataireRepository.countByUtilisateurIdAndStatutNotification(userId, StatutNotification.NON_LUE);
    }

    @Override
    @Transactional
    public NotificationDestinataireResponse markAsRead(Long notificationDestinataireId) {
        NotificationDestinataire notificationDestinataire = findNotificationDestinataire(notificationDestinataireId);

        if (notificationDestinataire.getStatutNotification() != StatutNotification.LUE) {
            notificationDestinataire.setStatutNotification(StatutNotification.LUE);
            notificationDestinataire.setDateLecture(LocalDateTime.now());
        }

        return toResponse(notificationDestinataireRepository.save(notificationDestinataire));
    }

    @Override
    @Transactional
    public NotificationDestinataireResponse markActionDone(Long notificationDestinataireId, String reponse) {
        NotificationDestinataire notificationDestinataire = findNotificationDestinataire(notificationDestinataireId);
        notificationDestinataire.setStatutActionNotif(StatutActionNotification.DONE);
        notificationDestinataire.setDateAction(LocalDateTime.now());
        notificationDestinataire.setReponse(reponse);
        return toResponse(notificationDestinataireRepository.save(notificationDestinataire));
    }

    @Override
    @Transactional
    public NotificationDestinataireResponse cancelNotificationForUser(Long notificationDestinataireId) {
        NotificationDestinataire notificationDestinataire = findNotificationDestinataire(notificationDestinataireId);
        notificationDestinataire.setStatutActionNotif(StatutActionNotification.CANCELLED);
        return toResponse(notificationDestinataireRepository.save(notificationDestinataire));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnedByUtilisateur(Long notificationDestinataireId, Long utilisateurId) {
        return notificationDestinataireRepository.findByIdAndUtilisateurId(notificationDestinataireId, utilisateurId).isPresent();
    }

    @Override
    public void notifierDemandeEntrepriseValidee(Long stagiaireId, Long demandeEntrepriseId, String nomEntreprise) {
        creerNotification(
                stagiaireId,
                "Demande d'ajout d'entreprise approuvee",
                "Votre demande d'ajout d'entreprise " + safeName(nomEntreprise) + " a ete approuvee avec succes.",
                TYPE_DEMANDE_ENTREPRISE_VALIDEE,
                demandeEntrepriseId,
                "DEMANDE_ENTREPRISE"
        );
    }

    @Override
    public void notifierDemandeEntrepriseRefusee(Long stagiaireId, Long demandeEntrepriseId, String motifRefus) {
        creerNotification(
                stagiaireId,
                "Demande d'ajout d'entreprise refusee",
                "Votre demande d'ajout d'entreprise a ete refusee.\nMotif : " + safeName(motifRefus),
                TYPE_DEMANDE_ENTREPRISE_REFUSEE,
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

    private NotificationDestinataire findNotificationDestinataire(Long notificationDestinataireId) {
        return notificationDestinataireRepository.findById(notificationDestinataireId)
                .orElseThrow(() -> new EntityNotFoundException("Notification destinataire introuvable"));
    }

    private NotificationDestinataireResponse toResponse(NotificationDestinataire notificationDestinataire) {
        Notification notification = notificationDestinataire.getNotification();
        return NotificationDestinataireResponse.builder()
                .notificationDestinataireId(notificationDestinataire.getId())
                .notificationId(notification != null ? notification.getId() : null)
                .titre(notification != null ? notification.getTitre() : null)
                .body(notification != null ? notification.getBody() : null)
                .typeNotification(notification != null ? notification.getTypeNotification() : null)
                .dateCreation(notification != null ? notification.getDateCreation() : null)
                .stageId(notification != null && notification.getStage() != null ? notification.getStage().getId() : null)
                .reunionId(notification != null && notification.getReunion() != null ? notification.getReunion().getId() : null)
                .reunionFinaleId(notification != null && notification.getReunionFinale() != null ? notification.getReunionFinale().getId() : null)
                .reunionHebdomadaireId(notification != null && notification.getReunionHebdomadaire() != null ? notification.getReunionHebdomadaire().getId() : null)
                .cahierStageId(notification != null && notification.getCahierStage() != null ? notification.getCahierStage().getId() : null)
                .ficheEvaluationId(notification != null && notification.getFicheEvaluation() != null ? notification.getFicheEvaluation().getId() : null)
                .conventionId(notification != null && notification.getConventionStage() != null ? notification.getConventionStage().getId() : null)
                .demandeId(notification != null && notification.getDemandeCreationCompteEntreprise() != null ? notification.getDemandeCreationCompteEntreprise().getId() : null)
                .statutNotification(notificationDestinataire.getStatutNotification())
                .statutActionNotif(notificationDestinataire.getStatutActionNotif())
                .dateLecture(notificationDestinataire.getDateLecture())
                .dateAction(notificationDestinataire.getDateAction())
                .reponse(notificationDestinataire.getReponse())
                .build();
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "selectionne";
        }
        return value.trim();
    }

    private void applyBusinessContext(Notification notification, CreateNotificationRequest request) {
        if (request.getStageId() != null) {
            Stage stage = stageRepository.findById(request.getStageId())
                    .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
            notification.setStage(stage);
        }

        if (request.getReunionId() != null) {
            Reunion reunion = reunionRepository.findById(request.getReunionId())
                    .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable"));
            notification.setReunion(reunion);
        }

        if (request.getReunionFinaleId() != null) {
            ReunionFinale reunionFinale = reunionFinaleRepository.findById(request.getReunionFinaleId())
                    .orElseThrow(() -> new EntityNotFoundException("Reunion finale introuvable"));
            notification.setReunionFinale(reunionFinale);
            notification.setReunion(reunionFinale);
        }

        if (request.getReunionHebdomadaireId() != null) {
            ReunionHebdomadaire reunionHebdomadaire = reunionHebdomadaireRepository.findById(request.getReunionHebdomadaireId())
                    .orElseThrow(() -> new EntityNotFoundException("Reunion hebdomadaire introuvable"));
            notification.setReunionHebdomadaire(reunionHebdomadaire);
            notification.setReunion(reunionHebdomadaire);
        }

        if (request.getCahierStageId() != null) {
            CahierStage cahierStage = cahierStageRepository.findById(request.getCahierStageId())
                    .orElseThrow(() -> new EntityNotFoundException("Cahier de stage introuvable"));
            notification.setCahierStage(cahierStage);
        }

        if (request.getFicheEvaluationId() != null) {
            FicheEvaluation ficheEvaluation = ficheEvaluationRepository.findById(request.getFicheEvaluationId())
                    .orElseThrow(() -> new EntityNotFoundException("Fiche d'evaluation introuvable"));
            notification.setFicheEvaluation(ficheEvaluation);
        }

        if (request.getConventionId() != null) {
            ConventionStage conventionStage = conventionStageRepository.findById(request.getConventionId())
                    .orElseThrow(() -> new EntityNotFoundException("Convention introuvable"));
            notification.setConventionStage(conventionStage);
        }

        if (request.getDemandeId() != null) {
            DemandeCreationCompteEntreprise demande = demandeCreationCompteEntrepriseRepository.findById(request.getDemandeId())
                    .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
            notification.setDemandeCreationCompteEntreprise(demande);
        }
    }

    private void applyBusinessContext(Notification notification, Long relatedEntityId, String relatedEntityType) {
        if (relatedEntityId == null || relatedEntityType == null || relatedEntityType.isBlank()) {
            return;
        }

        switch (relatedEntityType.trim().toUpperCase()) {
            case "STAGE" -> notification.setStage(
                    stageRepository.findById(relatedEntityId)
                            .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"))
            );
            case "REUNION" -> notification.setReunion(
                    reunionRepository.findById(relatedEntityId)
                            .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable"))
            );
            case "REUNION_FINALE" -> {
                ReunionFinale reunionFinale = reunionFinaleRepository.findById(relatedEntityId)
                        .orElseThrow(() -> new EntityNotFoundException("Reunion finale introuvable"));
                notification.setReunionFinale(reunionFinale);
                notification.setReunion(reunionFinale);
            }
            case "REUNION_HEBDOMADAIRE" -> {
                ReunionHebdomadaire reunionHebdomadaire = reunionHebdomadaireRepository.findById(relatedEntityId)
                        .orElseThrow(() -> new EntityNotFoundException("Reunion hebdomadaire introuvable"));
                notification.setReunionHebdomadaire(reunionHebdomadaire);
                notification.setReunion(reunionHebdomadaire);
            }
            case "CAHIER_STAGE" -> notification.setCahierStage(
                    cahierStageRepository.findById(relatedEntityId)
                            .orElseThrow(() -> new EntityNotFoundException("Cahier de stage introuvable"))
            );
            case "FICHE_EVALUATION" -> notification.setFicheEvaluation(
                    ficheEvaluationRepository.findById(relatedEntityId)
                            .orElseThrow(() -> new EntityNotFoundException("Fiche d'evaluation introuvable"))
            );
            case "CONVENTION", "CONVENTION_STAGE" -> notification.setConventionStage(
                    conventionStageRepository.findById(relatedEntityId)
                            .orElseThrow(() -> new EntityNotFoundException("Convention introuvable"))
            );
            case "DEMANDE", "DEMANDE_ENTREPRISE", "DEMANDE_CREATION_COMPTE_ENTREPRISE" -> notification.setDemandeCreationCompteEntreprise(
                    demandeCreationCompteEntrepriseRepository.findById(relatedEntityId)
                            .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"))
            );
            default -> {
                // Unknown business context: keep notification generic for compatibility.
            }
        }
    }
}
