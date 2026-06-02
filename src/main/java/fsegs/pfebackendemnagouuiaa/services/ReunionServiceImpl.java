package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionEligibleParticipantDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.exception.TechnicalOperationException;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionHebdomadaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.service.FinalMeetingUpdatePolicy;
import fsegs.pfebackendemnagouuiaa.service.MeetingVisibilityRules;
import fsegs.pfebackendemnagouuiaa.service.MeetingInvitationRules;
import fsegs.pfebackendemnagouuiaa.service.WeeklyMeetingObservationPolicy;
import fsegs.pfebackendemnagouuiaa.service.WeeklyMeetingParticipantPolicy;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReunionServiceImpl implements ReunionService {

    private static final Duration MIN_UPDATE_DELAY = Duration.ofHours(24);
    private static final String ERROR_REQUIRED = "Tous les champs obligatoires doivent étre renseignés.";
    private static final String ERROR_INVALID = "Les données saisies sont invalides ou hors période du stage.";
    private static final String ERROR_PERIOD =
            "La date de la réunion doit être comprise entre le début et la fin du stage.";
    private static final String ERROR_DUPLICATE_HEBDOMADAIRE =
            "Une réunion hebdomadaire existe déjà à cette date et heure pour ce stage.";
    private static final String ERROR_STAGE_ENDED =
            "Impossible de planifier une réunion : la période du stage est terminée.";
    private static final String ERROR_NOT_FOUND = "Réunion introuvable.";
    private static final String ERROR_FORBIDDEN_STAGE = "Accés non autorisé à ce stage.";
    private static final String ERROR_NOT_PARTICIPANT = "Accès refusé : vous n'êtes pas participant à cette réunion.";
    private static final String ERROR_DELETE_FORBIDDEN = "La suppression des réunions est interdite.";
    private static final String ERROR_TECHNICAL = "Une erreur technique est survenue lors de l'enregistrement ou de l'envoi des notifications.";
    private static final String ERROR_DELAY = "Une réunion doit étre planifiée ou modifiée au moins 24 heures avant son début.";

    private final ReunionRepository reunionRepository;
    private final ReunionHebdomadaireRepository reunionHebdomadaireRepository;
    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final ReunionMapper reunionMapper;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    @Transactional
    public ReunionDto create(ReunionDto dto) {
        validateWeeklyMeetingCreatePayload(dto);

        Stage stage = stageRepository.findByIdWithMeetingActors(dto.getStageId())
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + dto.getStageId()));

        Utilisateur supervisor = getAuthorizedSupervisorForStage(stage);
        WeeklyMeetingParticipantPolicy.assertEncadrantSupervisorCreates(supervisor);
        validateStageAllowsMeetingPlanning(stage);
        validateMeetingWithinStagePeriod(stage, dto.getDate());
        ensureMeetingDelayRespected(dto.getDate(), dto.getHeure());
        validateNoDuplicateMeeting(stage.getId(), dto.getDate(), dto.getHeure(), null);

        Reunion reunion = new ReunionHebdomadaire();
        reunion.setId(dto.getId());
        reunion.setDate(dto.getDate());
        reunion.setHeure(dto.getHeure());
        reunion.setCompteRendu(normalizeNullableText(dto.getCompteRendu()));
        reunion.setStage(stage);
        reunion.setCahierStage(resolveOrCreateCahierStage(stage));
        reunion.setEncadrantCreateurId(supervisor.getId());
        reunion.setTypeEncadrantCreateur(resolveSupervisorType(supervisor.getRole()));
        reunion.setNomEncadrantCreateur(formatFullName(supervisor));
        reunion.setNumReunion(normalizeNullableText(dto.getNumReunion()) != null
                ? dto.getNumReunion().trim()
                : generateNumReunion(stage.getId()));
        reunion.setParticipants(WeeklyMeetingParticipantPolicy.resolveParticipants(stage, supervisor));

        try {
            Reunion saved = reunionRepository.save(reunion);
            notifierCreationMeeting(saved);
            return mapReunionForCurrentUser(saved);
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la creation de la reunion pour le stage {}", dto.getStageId(), ex);
            throw new TechnicalOperationException(ERROR_TECHNICAL, ex);
        }
    }

    private String generateNumReunion(Long stageId) {
        long count = reunionRepository.findByStageId(stageId).size() + 1;
        String numero;

        do {
            numero = String.format("REU-STAGE-%d-%03d", stageId, count++);
        } while (reunionRepository.findByNumReunion(numero).isPresent());

        return numero;
    }

    @Override
    public ReunionDto getById(Long id) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ERROR_NOT_FOUND));
        ensureCanAccessReunion(reunion);

        return mapReunionForCurrentUser(reunion);
    }

    @Override
    public List<ReunionDto> getAll() {
        Utilisateur authenticatedUser = getOptionalAuthenticatedUser();
        if (MeetingVisibilityRules.isSupervisor(authenticatedUser) && authenticatedUser.getId() != null) {
            return reunionRepository.findByEncadrantCreateurIdOrderByDateDescHeureDesc(authenticatedUser.getId())
                    .stream()
                    .filter(reunion -> !(reunion instanceof ReunionFinale))
                    .filter(reunion -> MeetingVisibilityRules.canSupervisorAccessMeeting(
                            authenticatedUser, reunion, reunion.getStage()))
                    .map(this::mapReunionForCurrentUser)
                    .toList();
        }

        return reunionRepository.findAllByOrderByDateDescHeureDesc()
                .stream()
                .map(this::mapReunionForCurrentUser)
                .toList();
    }

    @Override
    public List<ReunionDto> getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + stageId));
        ensureCanAccessMeetingStage(stage);

        Utilisateur authenticatedUser = getOptionalAuthenticatedUser();
        List<Reunion> reunionsStage = reunionRepository.findByStageIdOrderByDateDescHeureDesc(stageId);
        if (MeetingVisibilityRules.isSupervisor(authenticatedUser) && authenticatedUser.getId() != null) {
            reunionsStage = reunionsStage.stream()
                    .filter(reunion -> MeetingVisibilityRules.canSupervisorAccessMeeting(
                            authenticatedUser, reunion, stage))
                    .toList();
        }

        List<ReunionDto> reunions = reunionsStage.stream()
                .map(this::mapReunionForCurrentUser)
                .toList();

        log.info("Chargement des reunions pour le stage {} -> {} reunion(s)", stageId, reunions.size());
        if (reunions.isEmpty()) {
            log.warn("Aucune reunion retournee pour le stage {} via /api/reunions/stage/{}", stageId, stageId);
        }

        return reunions;
    }

    @Override
    public List<ReunionEligibleParticipantDto> getEligibleParticipantsForStage(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + stageId));
        getAuthorizedSupervisorForStage(stage);

        List<ReunionEligibleParticipantDto> participants = new ArrayList<>();
        appendEligibleParticipant(participants, stage.getStagiaire(), Role.STAGIAIRE, "Stagiaire");
        appendEligibleParticipant(participants, stage.getEncadrantAcademique(), Role.ENCADRANT_ACADEMIQUE, "Encadrant académique");
        appendEligibleParticipant(participants, stage.getEncadrantProfessionnel(), Role.ENCADRANT_PROFESSIONNEL, "Encadrant professionnel");
        return participants;
    }

    @Override
    public List<ReunionDto> getPourStagiaireAuthentifie() {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new RuntimeException("Utilisateur authentifie introuvable."));

        if (utilisateur.getRole() != Role.STAGIAIRE) {
            throw new AccessDeniedException("Acces refuse : role stagiaire requis.");
        }

        return reunionRepository.findByStageStagiaireIdOrderByDateDescHeureDesc(utilisateur.getId())
                .stream()
                .map(this::mapReunionForCurrentUser)
                .toList();
    }

    @Override
    public List<ReunionDto> getPourEntrepriseAuthentifiee() {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new RuntimeException("Utilisateur authentifie introuvable."));

        ResponsableEntreprise responsable = resolveResponsableEntreprise(utilisateur);
        if (responsable.getEntreprise() == null || responsable.getEntreprise().getId() == null) {
            log.warn(
                    "Aucune reunion retournee: utilisateur={} email={} role={} sans entreprise rattachee",
                    utilisateur.getId(),
                    utilisateur.getEmail(),
                    utilisateur.getRole()
            );
            return List.of();
        }

        Long entrepriseId = responsable.getEntreprise().getId();
        List<Stage> stagesEntreprise = stageRepository.findByEntrepriseId(entrepriseId);
        List<Long> stageIds = stagesEntreprise.stream().map(Stage::getId).toList();
        long stagesDuRepresentant = stagesEntreprise.stream()
                .filter(stage -> stage.getTuteurEntreprise() != null && responsable.getId().equals(stage.getTuteurEntreprise().getId()))
                .count();

        List<ReunionDto> reunions = reunionRepository.findByStageEntrepriseIdOrderByDateDescHeureDesc(entrepriseId)
                .stream()
                .map(this::mapReunionForCurrentUser)
                .toList();

        String repartitionParType = reunions.stream()
                .collect(Collectors.groupingBy(ReunionDto::getTypeReunion, Collectors.counting()))
                .toString();

        log.info(
                "Reunions entreprise chargees: utilisateur={} email={} entrepriseId={} entrepriseNom={} stagesEntreprise={} stagesTuteur={} reunions={} types={}",
                utilisateur.getId(),
                utilisateur.getEmail(),
                entrepriseId,
                responsable.getEntreprise().getNom(),
                stagesEntreprise.size(),
                stagesDuRepresentant,
                reunions.size(),
                repartitionParType
        );

        if (reunions.isEmpty()) {
            if (stageIds.isEmpty()) {
                log.warn(
                        "Aucune reunion retournee pour l'entreprise {} car aucun stage n'est rattache a cette entreprise",
                        entrepriseId
                );
            } else {
                log.warn(
                        "Aucune reunion retournee pour l'entreprise {} alors que des stages existent. stageIds={}",
                        entrepriseId,
                        stageIds
                );
            }
        }

        return reunions;
    }

    @Override
    public ReunionDto update(Long id, ReunionDto dto) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ERROR_NOT_FOUND));

        if (reunion instanceof ReunionFinale finale) {
            return updateFinalMeetingAllowedFields(finale, dto);
        }

        validateWeeklyMeetingUpdatePayload(dto);

        Utilisateur supervisor = getAuthorizedSupervisorForStage(reunion.getStage());
        ensureMeetingCreator(reunion, supervisor);
        ensureMeetingCanBeModified(reunion.getDate(), reunion.getHeure());

        if (dto.getNumReunion() != null && !dto.getNumReunion().isBlank()) {
            reunion.setNumReunion(dto.getNumReunion().trim());
        }
        reunion.setDate(dto.getDate());
        reunion.setHeure(dto.getHeure());
        reunion.setCompteRendu(normalizeNullableText(dto.getCompteRendu()));

        if (dto.getStageId() != null) {
            Stage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + dto.getStageId()));
            getAuthorizedSupervisorForStage(stage);
            reunion.setStage(stage);
            reunion.setCahierStage(resolveOrCreateCahierStage(stage));
        }

        if (reunion.getStage() == null || reunion.getStage().getId() == null) {
            throw new IllegalArgumentException("Le stage est obligatoire");
        }

        validateStageAllowsMeetingPlanning(reunion.getStage());
        validateMeetingWithinStagePeriod(reunion.getStage(), reunion.getDate());
        ensureMeetingDelayRespected(reunion.getDate(), reunion.getHeure());
        validateNoDuplicateMeeting(reunion.getStage().getId(), reunion.getDate(), reunion.getHeure(), reunion.getId());
        if (reunion.getCahierStage() == null) {
            reunion.setCahierStage(resolveOrCreateCahierStage(reunion.getStage()));
        }

        try {
            Reunion updated = reunionRepository.save(reunion);
            notifierMeetingUpdate(updated);
            return mapReunionForCurrentUser(updated);
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la modification de la reunion {}", id, ex);
            throw new TechnicalOperationException(ERROR_TECHNICAL, ex);
        }
    }

    @Override
    public ReunionDto updateObservation(Long id, String observation) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        ensureCanAccessReunion(reunion);
        Utilisateur supervisor = getAuthorizedSupervisorForStage(reunion.getStage());
        if (reunion instanceof ReunionFinale) {
            throw new IllegalArgumentException(FinalMeetingUpdatePolicy.ERROR_OBSERVATION_FORBIDDEN);
        }
        validateStageAllowsSupervisorActions(reunion.getStage());
        WeeklyMeetingObservationPolicy.assertCanManageObservation(supervisor, reunion);
        WeeklyMeetingObservationPolicy.writeObservationForUser(
                reunion,
                supervisor,
                normalizeRequiredText(observation, ERROR_REQUIRED)
        );
        Reunion updated = reunionRepository.save(reunion);
        notifierParticipants(updated, "Observation de reunion mise a jour",
                "L'observation de la reunion de suivi " + updated.getNumReunion() + " a ete mise a jour.");
        return mapReunionForCurrentUser(updated);
    }

    @Override
    public ReunionDto updateCompteRendu(Long id, String compteRendu) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        getAuthorizedStagiaireForMeeting(reunion);
        reunion.setCompteRendu(normalizeNullableText(compteRendu));
        Reunion updated = reunionRepository.save(reunion);
        notifierParticipants(updated, "Compte rendu de reunion mis a jour",
                "Le compte rendu de la reunion de suivi " + updated.getNumReunion() + " a ete mis a jour.");

        return mapReunionForCurrentUser(updated);
    }

    @Override
    public void delete(Long id) {
        throw new IllegalStateException(ERROR_DELETE_FORBIDDEN);
    }

    private ResponsableEntreprise resolveResponsableEntreprise(Utilisateur utilisateur) {
        if (utilisateur instanceof ResponsableEntreprise responsableEntreprise) {
            return responsableEntreprise;
        }

        return responsableEntrepriseRepository.findByEmailIgnoreCase(utilisateur.getEmail())
                .orElseThrow(() -> new RuntimeException("Responsable entreprise introuvable pour l'utilisateur connecte."));
    }

    private void validateMeetingWithinStagePeriod(Stage stage, LocalDate meetingDate) {
        LocalDate dateDebut = stage.getDateDebut();
        LocalDate dateFin = resolveStageEndDate(stage);
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }

        if (meetingDate == null
                || meetingDate.isBefore(dateDebut)
                || meetingDate.isAfter(dateFin)) {
            throw new IllegalArgumentException(ERROR_PERIOD);
        }
    }

    private void validateNoDuplicateMeeting(Long stageId, LocalDate date, LocalTime heure, Long currentMeetingId) {
        if (stageId == null || date == null || heure == null) {
            return;
        }

        boolean exists = currentMeetingId == null
                ? reunionHebdomadaireRepository.existsByStageIdAndDateAndHeure(stageId, date, heure)
                : reunionHebdomadaireRepository.existsByStageIdAndDateAndHeureAndIdNot(
                        stageId, date, heure, currentMeetingId);

        if (exists) {
            throw new IllegalArgumentException(ERROR_DUPLICATE_HEBDOMADAIRE);
        }
    }

    /**
     * Autorise la planification ou la modification d'une réunion hebdomadaire :
     * stage non annulé/refusé, dates de stage connues, période du stage non terminée.
     * La date du jour peut être antérieure à {@code dateDebut} (stage à venir).
     */
    private void validateStageAllowsMeetingPlanning(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
        StatutStage statut = stage.getStatut();
        if (statut == StatutStage.REFUSE || statut == StatutStage.ANNULE) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }

        LocalDate dateDebut = stage.getDateDebut();
        LocalDate dateFin = resolveStageEndDate(stage);
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(dateFin)) {
            throw new IllegalArgumentException(ERROR_STAGE_ENDED);
        }
    }

    /**
     * Autorise les actions encadrant (observations, etc.) lorsque la date du jour
     * est comprise entre dateDebut et dateFin du stage (bornes incluses).
     */
    private void validateStageAllowsSupervisorActions(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
        StatutStage statut = stage.getStatut();
        if (statut == StatutStage.REFUSE || statut == StatutStage.ANNULE) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }

        LocalDate dateDebut = stage.getDateDebut();
        LocalDate dateFin = resolveStageEndDate(stage);
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(dateDebut) || today.isAfter(dateFin)) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
    }

    private LocalDate resolveStageEndDate(Stage stage) {
        if (stage == null) {
            return null;
        }
        if (stage.getDateFin() != null) {
            return stage.getDateFin();
        }
        if (stage.getDateDebut() != null && stage.getDuree() != null) {
            return stage.getDateDebut().plusMonths(stage.getDuree().longValue());
        }
        return null;
    }

    private void ensureMeetingCanBeModified(LocalDate date, LocalTime heure) {
        if (date == null || heure == null) {
            throw new IllegalArgumentException(ERROR_REQUIRED);
        }

        LocalDateTime meetingStart = LocalDateTime.of(date, heure);
        if (Duration.between(LocalDateTime.now(), meetingStart).compareTo(MIN_UPDATE_DELAY) < 0) {
            throw new IllegalArgumentException(ERROR_DELAY);
        }
    }

    private Utilisateur getAuthorizedSupervisorForStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException(ERROR_FORBIDDEN_STAGE);
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException(ERROR_FORBIDDEN_STAGE));

        boolean academicSupervisor = utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                && stage.getEncadrantAcademique() != null
                && utilisateur.getId().equals(stage.getEncadrantAcademique().getId());
        boolean professionalSupervisor = utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage.getEncadrantProfessionnel() != null
                && utilisateur.getId().equals(stage.getEncadrantProfessionnel().getId());

        if (!academicSupervisor && !professionalSupervisor) {
            throw new AccessDeniedException(ERROR_FORBIDDEN_STAGE);
        }

        return utilisateur;
    }

    private Utilisateur getAuthorizedStagiaireForMeeting(Reunion reunion) {
        if (reunion == null || reunion.getStage() == null || reunion.getStage().getId() == null) {
            throw new AccessDeniedException("Aucune reunion de stage valide n'est associee.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        boolean isLinkedStagiaire = utilisateur.getRole() == Role.STAGIAIRE
                && reunion.getStage().getStagiaire() != null
                && utilisateur.getId().equals(reunion.getStage().getStagiaire().getId());

        if (!isLinkedStagiaire) {
            throw new AccessDeniedException("Seul le stagiaire concerne peut modifier le compte rendu.");
        }

        return utilisateur;
    }

    private void notifierCreationMeeting(Reunion reunion) {
        Stage stage = reunion.getStage();
        if (stage != null && stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            notificationService.notifierStagiaireReunionFixee(
                    stage.getStagiaire().getId(),
                    reunion.getId(),
                    buildStudentMeetingNotification(reunion)
            );
        }

        Set<Long> recipients = collectParticipantIds(reunion);
        Long stagiaireId = stage != null && stage.getStagiaire() != null ? stage.getStagiaire().getId() : null;
        recipients.stream()
                .filter(userId -> stagiaireId == null || !stagiaireId.equals(userId))
                .forEach(userId -> notificationService.creerNotification(
                        userId,
                        "Nouvelle reunion de suivi",
                        "Une reunion de suivi a ete programmee le " + reunion.getDate() + " a " + reunion.getHeure() + ".",
                        "REUNION_SUIVI",
                        reunion.getId(),
                        "REUNION"
                ));
    }

    private void notifierMeetingUpdate(Reunion reunion) {
        String message = "La reunion hebdomadaire " + safeText(reunion.getNumReunion(), "de suivi")
                + " a ete modifiee. Nouvelle date : " + reunion.getDate() + " a " + reunion.getHeure() + ".";
        notifierParticipants(reunion, "Reunion hebdomadaire modifiee", message);
    }

    private String buildStudentMeetingNotification(Reunion reunion) {
        String typeEncadrant = "ACADEMIQUE".equalsIgnoreCase(reunion.getTypeEncadrantCreateur())
                ? "académique"
                : "professionnel";
        String nomEncadrant = safeText(reunion.getNomEncadrantCreateur(), "Non renseigné");
        String date = reunion.getDate() != null ? reunion.getDate().toString() : "date non renseignée";
        String heure = reunion.getHeure() != null ? reunion.getHeure().toString() : "heure non renseignée";
        return "Une nouvelle réunion a été planifiée avec votre encadrant "
                + typeEncadrant + " " + nomEncadrant + " le " + date + " à " + heure + ".";
    }

    private String resolveSupervisorType(Role role) {
        return role == Role.ENCADRANT_ACADEMIQUE ? "ACADEMIQUE" : "PROFESSIONNEL";
    }

    private String formatFullName(Utilisateur utilisateur) {
        String fullName = ((utilisateur.getPrenom() != null ? utilisateur.getPrenom() : "") + " "
                + (utilisateur.getNom() != null ? utilisateur.getNom() : "")).trim();
        return fullName.isBlank() ? safeText(utilisateur.getEmail(), "Encadrant") : fullName;
    }

    private ReunionDto updateFinalMeetingAllowedFields(ReunionFinale reunion, ReunionDto dto) {
        Utilisateur supervisor = getAuthorizedSupervisorForStage(reunion.getStage());
        ensureSupervisorIsMeetingParticipant(reunion, supervisor);

        FinalMeetingUpdatePolicy.assertLockedFieldsUnchanged(
                reunion,
                dto != null ? dto.getStageId() : null,
                dto != null ? dto.getNumReunion() : null,
                dto != null ? dto.getCompteRendu() : null,
                dto != null ? dto.getParticipantIds() : null
        );
        FinalMeetingUpdatePolicy.assertOnlyHeureMayChange(
                reunion,
                dto != null ? dto.getDate() : null,
                dto != null ? dto.getHeure() : null,
                dto != null ? dto.getObservation() : null
        );

        reunion.setHeure(dto.getHeure());

        Stage stage = reunion.getStage();
        if (stage == null || stage.getId() == null) {
            throw new IllegalArgumentException("Le stage est obligatoire");
        }

        validateMeetingWithinStagePeriod(stage, reunion.getDate());
        validateNoDuplicateMeeting(stage.getId(), reunion.getDate(), reunion.getHeure(), reunion.getId());

        try {
            Reunion updated = reunionRepository.save(reunion);
            notifierMeetingUpdate(updated);
            return mapReunionForCurrentUser(updated);
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la modification de l'horaire de la reunion finale {}", reunion.getId(), ex);
            throw new TechnicalOperationException(ERROR_TECHNICAL, ex);
        }
    }

    private void appendEligibleParticipant(List<ReunionEligibleParticipantDto> target,
                                           Utilisateur utilisateur,
                                           Role expectedRole,
                                           String roleLabel) {
        if (!MeetingInvitationRules.isEligibleMeetingParticipant(utilisateur)
                || utilisateur.getId() == null) {
            return;
        }
        if (utilisateur.getRole() != expectedRole) {
            return;
        }

        target.add(ReunionEligibleParticipantDto.builder()
                .id(utilisateur.getId())
                .fullName(formatFullName(utilisateur))
                .email(safeText(utilisateur.getEmail(), ""))
                .role(expectedRole.name())
                .roleLabel(roleLabel)
                .build());
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private void validateWeeklyMeetingCreatePayload(ReunionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException(ERROR_REQUIRED);
        }

        if (dto.getStageId() == null
                || dto.getDate() == null
                || dto.getHeure() == null
                || normalizeNullableText(dto.getTypeReunion()) == null) {
            throw new IllegalArgumentException(ERROR_REQUIRED);
        }

        String normalizedType = dto.getTypeReunion().trim().toUpperCase();
        if (!"HEBDOMADAIRE".equals(normalizedType)) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
    }

    private void validateWeeklyMeetingUpdatePayload(ReunionDto dto) {
        validateWeeklyMeetingCreatePayload(dto);
    }

    private ReunionDto mapReunionForCurrentUser(Reunion reunion) {
        ReunionDto dto = reunionMapper.toDto(reunion);
        if (dto == null || reunion instanceof ReunionFinale) {
            return dto;
        }

        Utilisateur utilisateur = getOptionalAuthenticatedUser();
        if (utilisateur == null) {
            dto.setObservation(null);
            return dto;
        }

        dto.setObservation(WeeklyMeetingObservationPolicy.readObservationForUser(reunion, utilisateur));
        return dto;
    }

    private void ensureMeetingDelayRespected(LocalDate date, LocalTime heure) {
        if (date == null || heure == null) {
            throw new IllegalArgumentException(ERROR_REQUIRED);
        }

        LocalDateTime meetingStart = LocalDateTime.of(date, heure);
        if (Duration.between(LocalDateTime.now(), meetingStart).compareTo(MIN_UPDATE_DELAY) < 0) {
            throw new IllegalArgumentException(ERROR_DELAY);
        }
    }

    private void ensureCanAccessReunion(Reunion reunion) {
        if (reunion == null) {
            throw new EntityNotFoundException(ERROR_NOT_FOUND);
        }

        Utilisateur utilisateur = getOptionalAuthenticatedUser();
        if (utilisateur == null) {
            return;
        }

        if (isSupervisor(utilisateur)) {
            if (!MeetingVisibilityRules.canSupervisorAccessMeeting(utilisateur, reunion, reunion.getStage())) {
                throw new AccessDeniedException(MeetingVisibilityRules.ERROR_NOT_MEETING_CREATOR);
            }
            return;
        }

        if (utilisateur.getRole() == Role.STAGIAIRE) {
            boolean participant = isMeetingParticipant(reunion, utilisateur);
            boolean stageOwner = reunion.getStage() != null
                    && reunion.getStage().getStagiaire() != null
                    && Objects.equals(utilisateur.getId(), reunion.getStage().getStagiaire().getId());
            if (!participant && !stageOwner) {
                throw new AccessDeniedException(ERROR_FORBIDDEN_STAGE);
            }
        }
    }

    private void ensureCanAccessMeetingStage(Stage stage) {
        Utilisateur utilisateur = getOptionalAuthenticatedUser();
        if (utilisateur == null) {
            return;
        }

        if (isSupervisor(utilisateur)) {
            if (!canSupervisorAccessStage(utilisateur, stage)) {
                throw new AccessDeniedException(ERROR_FORBIDDEN_STAGE);
            }
            return;
        }

        if (utilisateur.getRole() == Role.STAGIAIRE) {
            boolean canAccess = stage != null
                    && stage.getStagiaire() != null
                    && Objects.equals(utilisateur.getId(), stage.getStagiaire().getId());
            if (!canAccess) {
                throw new AccessDeniedException(ERROR_FORBIDDEN_STAGE);
            }
        }
    }

    private boolean isMeetingParticipant(Reunion reunion, Utilisateur utilisateur) {
        if (reunion == null || utilisateur == null || utilisateur.getId() == null) {
            return false;
        }
        if (reunion.getParticipants() == null || reunion.getParticipants().isEmpty()) {
            return false;
        }
        return reunion.getParticipants().stream()
                .anyMatch(participant -> participant != null
                        && Objects.equals(participant.getId(), utilisateur.getId()));
    }

    private void ensureSupervisorIsMeetingParticipant(Reunion reunion, Utilisateur supervisor) {
        if (!isMeetingParticipant(reunion, supervisor)) {
            throw new AccessDeniedException(ERROR_NOT_PARTICIPANT);
        }
    }

    private void ensureMeetingCreator(Reunion reunion, Utilisateur utilisateur) {
        if (!MeetingVisibilityRules.isMeetingCreator(reunion, utilisateur)) {
            throw new AccessDeniedException(MeetingVisibilityRules.ERROR_NOT_MEETING_CREATOR);
        }
    }

    private void ensureCreatorIsParticipant(Utilisateur supervisor, Reunion reunion) {
        if (supervisor == null || supervisor.getId() == null) {
            throw new AccessDeniedException(ERROR_NOT_PARTICIPANT);
        }
        if (!isMeetingParticipant(reunion, supervisor)) {
            throw new IllegalArgumentException(
                    "L'encadrant qui crée ou modifie la réunion doit figurer parmi les participants.");
        }
    }

    private boolean isSupervisor(Utilisateur utilisateur) {
        return utilisateur != null
                && (utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                || utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL);
    }

    private boolean canSupervisorAccessStage(Utilisateur utilisateur, Stage stage) {
        if (utilisateur == null || stage == null) {
            return false;
        }

        boolean academicSupervisor = utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                && stage.getEncadrantAcademique() != null
                && Objects.equals(utilisateur.getId(), stage.getEncadrantAcademique().getId());
        boolean professionalSupervisor = utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage.getEncadrantProfessionnel() != null
                && Objects.equals(utilisateur.getId(), stage.getEncadrantProfessionnel().getId());

        return academicSupervisor || professionalSupervisor;
    }

    private Utilisateur getOptionalAuthenticatedUser() {
        return jwtService.getAuthenticatedUtilisateur().orElse(null);
    }

    private void notifierParticipants(Reunion reunion, String title, String message) {
        Set<Long> recipients = collectParticipantIds(reunion);
        recipients.forEach(userId -> notificationService.creerNotification(
                userId,
                title,
                message,
                "REUNION_SUIVI",
                reunion.getId(),
                "REUNION"
        ));
    }

    private Set<Long> collectParticipantIds(Reunion reunion) {
        Set<Long> recipients = new LinkedHashSet<>();
        Stage stage = reunion.getStage();
        if (stage != null) {
            addRecipient(recipients, stage.getStagiaire());
            addRecipient(recipients, stage.getEncadrantAcademique());
            addRecipient(recipients, stage.getEncadrantProfessionnel());
        }

        if (reunion.getParticipants() != null) {
            reunion.getParticipants().forEach(participant -> addRecipient(recipients, participant));
        }

        return recipients;
    }

    private void addRecipient(Set<Long> recipients, Utilisateur utilisateur) {
        if (utilisateur != null && utilisateur.getId() != null) {
            recipients.add(utilisateur.getId());
        }
    }

    private Set<Utilisateur> resolveParticipants(Collection<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueParticipantIds = participantIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Utilisateur> participants = utilisateurRepository.findAllById(uniqueParticipantIds);
        if (participants.size() != uniqueParticipantIds.size()) {
            throw new EntityNotFoundException("Un ou plusieurs participants sont introuvables.");
        }

        MeetingInvitationRules.assertNoCompanyManagerInvitation(participants);
        return new HashSet<>(MeetingInvitationRules.retainEligibleParticipants(participants));
    }

    private CahierStage resolveOrCreateCahierStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return null;
        }

        if (stage.getCahierStage() != null) {
            return stage.getCahierStage();
        }

        return cahierStageRepository.findByStageId(stage.getId())
                .orElseGet(() -> {
                    if (!canAutoCreateCahierStage(stage)) {
                        return null;
                    }

                    CahierStage cahierStage = new CahierStage();
                    cahierStage.setStage(stage);
                    cahierStage.setDateGeneration(LocalDate.now());
                    CahierStage saved = cahierStageRepository.save(cahierStage);
                    stage.setCahierStage(saved);
                    return saved;
                });
    }

    private boolean canAutoCreateCahierStage(Stage stage) {
        if (stage == null || stage.getStatut() == null) {
            return false;
        }

        return stage.getStatut() == StatutStage.EN_COURS;
    }
}
