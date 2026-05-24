package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.exception.TechnicalOperationException;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final String ERROR_NOT_FOUND = "Réunion introuvable.";
    private static final String ERROR_FORBIDDEN_STAGE = "Accés non autorisé à ce stage.";
    private static final String ERROR_DELETE_FORBIDDEN = "La suppression des réunions est interdite.";
    private static final String ERROR_TECHNICAL = "Une erreur technique est survenue lors de l'enregistrement ou de l'envoi des notifications.";
    private static final String ERROR_DELAY = "Une réunion doit étre planifiée ou modifiée au moins 24 heures avant son début.";

    private final ReunionRepository reunionRepository;
    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final ReunionMapper reunionMapper;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    public ReunionDto create(ReunionDto dto) {
        validateMeetingPayload(dto);

        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + dto.getStageId()));

        Utilisateur supervisor = getAuthorizedSupervisorForStage(stage);
        validateActiveStage(stage);
        validateMeetingWithinStagePeriod(stage, dto.getDate());
        ensureMeetingDelayRespected(dto.getDate(), dto.getHeure());
        validateNoDuplicateMeeting(stage.getId(), dto.getDate(), dto.getHeure(), null);

        Reunion reunion = new ReunionHebdomadaire();
        reunion.setId(dto.getId());
        reunion.setDate(dto.getDate());
        reunion.setHeure(dto.getHeure());
        reunion.setObservation(normalizeRequiredText(dto.getObservation(), ERROR_REQUIRED));
        reunion.setCompteRendu(normalizeNullableText(dto.getCompteRendu()));
        reunion.setStage(stage);
        reunion.setCahierStage(resolveOrCreateCahierStage(stage));
        reunion.setEncadrantCreateurId(supervisor.getId());
        reunion.setTypeEncadrantCreateur(resolveSupervisorType(supervisor.getRole()));
        reunion.setNomEncadrantCreateur(formatFullName(supervisor));
        reunion.setNumReunion(normalizeNullableText(dto.getNumReunion()) != null
                ? dto.getNumReunion().trim()
                : generateNumReunion(stage.getId()));
        reunion.setParticipants(resolveParticipants(dto.getParticipantIds()));

        try {
            Reunion saved = reunionRepository.save(reunion);
            notifierCreationMeeting(saved);
            return reunionMapper.toDto(saved);
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
        ensureCanAccessMeetingStage(reunion.getStage());

        return reunionMapper.toDto(reunion);
    }

    @Override
    public List<ReunionDto> getAll() {
        Utilisateur authenticatedUser = getOptionalAuthenticatedUser();
        if (isSupervisor(authenticatedUser)) {
            return stageRepository.findAll().stream()
                    .filter(stage -> canSupervisorAccessStage(authenticatedUser, stage))
                    .map(Stage::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .flatMap(stageId -> reunionRepository.findByStageIdOrderByDateDescHeureDesc(stageId).stream())
                    .map(reunionMapper::toDto)
                    .toList();
        }

        return reunionRepository.findAllByOrderByDateDescHeureDesc()
                .stream()
                .map(reunionMapper::toDto)
                .toList();
    }

    @Override
    public List<ReunionDto> getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + stageId));
        ensureCanAccessMeetingStage(stage);

        List<ReunionDto> reunions = reunionRepository.findByStageIdOrderByDateDescHeureDesc(stageId)
                .stream()
                .map(reunionMapper::toDto)
                .toList();

        log.info("Chargement des reunions pour le stage {} -> {} reunion(s)", stageId, reunions.size());
        if (reunions.isEmpty()) {
            log.warn("Aucune reunion retournee pour le stage {} via /api/reunions/stage/{}", stageId, stageId);
        }

        return reunions;
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
                .map(reunionMapper::toDto)
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
                .map(reunionMapper::toDto)
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
        validateMeetingPayload(dto);

        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ERROR_NOT_FOUND));

        getAuthorizedSupervisorForStage(reunion.getStage());
        ensureMeetingCanBeModified(reunion.getDate(), reunion.getHeure());

        if (dto.getNumReunion() != null && !dto.getNumReunion().isBlank()) {
            reunion.setNumReunion(dto.getNumReunion().trim());
        }
        reunion.setDate(dto.getDate());
        reunion.setHeure(dto.getHeure());
        reunion.setObservation(normalizeRequiredText(dto.getObservation(), ERROR_REQUIRED));
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

        validateActiveStage(reunion.getStage());
        validateMeetingWithinStagePeriod(reunion.getStage(), reunion.getDate());
        ensureMeetingDelayRespected(reunion.getDate(), reunion.getHeure());
        validateNoDuplicateMeeting(reunion.getStage().getId(), reunion.getDate(), reunion.getHeure(), reunion.getId());
        if (reunion.getCahierStage() == null) {
            reunion.setCahierStage(resolveOrCreateCahierStage(reunion.getStage()));
        }

        if (dto.getParticipantIds() != null) {
            reunion.setParticipants(resolveParticipants(dto.getParticipantIds()));
        }

        try {
            Reunion updated = reunionRepository.save(reunion);
            notifierMeetingUpdate(updated);
            return reunionMapper.toDto(updated);
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la modification de la reunion {}", id, ex);
            throw new TechnicalOperationException(ERROR_TECHNICAL, ex);
        }
    }

    @Override
    public ReunionDto updateObservation(Long id, String observation) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        getAuthorizedSupervisorForStage(reunion.getStage());
        reunion.setObservation(normalizeNullableText(observation));
        Reunion updated = reunionRepository.save(reunion);
        notifierParticipants(updated, "Observation de reunion mise a jour",
                "L'observation de la reunion de suivi " + updated.getNumReunion() + " a ete mise a jour.");
        return reunionMapper.toDto(updated);
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

        return reunionMapper.toDto(updated);
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
        if (stage.getDateDebut() == null || stage.getDateFin() == null) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }

        if (meetingDate == null
                || meetingDate.isBefore(stage.getDateDebut())
                || meetingDate.isAfter(stage.getDateFin())) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
    }

    private void validateNoDuplicateMeeting(Long stageId, LocalDate date, LocalTime heure, Long currentMeetingId) {
        if (stageId == null || date == null || heure == null) {
            return;
        }

        boolean exists = currentMeetingId == null
                ? reunionRepository.existsByStageIdAndDateAndHeure(stageId, date, heure)
                : reunionRepository.existsByStageIdAndDateAndHeureAndIdNot(stageId, date, heure, currentMeetingId);

        if (exists) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
    }

    private void validateActiveStage(Stage stage) {
        if (stage == null || stage.getStatut() != StatutStage.EN_COURS) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
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

    private void validateMeetingPayload(ReunionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException(ERROR_REQUIRED);
        }

        if (dto.getStageId() == null
                || dto.getDate() == null
                || dto.getHeure() == null
                || normalizeNullableText(dto.getTypeReunion()) == null
                || normalizeNullableText(dto.getObservation()) == null
                || dto.getParticipantIds() == null
                || dto.getParticipantIds().isEmpty()) {
            throw new IllegalArgumentException(ERROR_REQUIRED);
        }

        String normalizedType = dto.getTypeReunion().trim().toUpperCase();
        if (!"HEBDOMADAIRE".equals(normalizedType)) {
            throw new IllegalArgumentException(ERROR_INVALID);
        }
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

        return new HashSet<>(participants);
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
