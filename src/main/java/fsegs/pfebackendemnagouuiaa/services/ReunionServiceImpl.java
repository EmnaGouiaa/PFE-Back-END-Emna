package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionMapper;
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
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReunionServiceImpl implements ReunionService {

    private static final Duration MIN_UPDATE_DELAY = Duration.ofHours(24);

    private final ReunionRepository reunionRepository;
    private final StageRepository stageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final ReunionMapper reunionMapper;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    public ReunionDto create(ReunionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Les donnees de la reunion sont obligatoires.");
        }

        if (dto.getStageId() == null) {
            throw new IllegalArgumentException("Le stage est obligatoire");
        }

        Reunion reunion = reunionMapper.toEntity(dto);
        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + dto.getStageId()));

        Utilisateur supervisor = getAuthorizedSupervisorForStage(stage);
        validateActiveStage(stage);
        reunion.setStage(stage);
        reunion.setEncadrantCreateurId(supervisor.getId());
        reunion.setTypeEncadrantCreateur(resolveSupervisorType(supervisor.getRole()));
        reunion.setNomEncadrantCreateur(formatFullName(supervisor));
        validateMeetingWithinStagePeriod(stage, dto.getDate());
        validateNoDuplicateMeeting(stage.getId(), dto.getDate(), dto.getHeure(), null);

        if (dto.getNumReunion() == null || dto.getNumReunion().isBlank()) {
            reunion.setNumReunion(generateNumReunion(stage.getId()));
        } else {
            reunion.setNumReunion(dto.getNumReunion().trim());
        }

        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            Set<Utilisateur> participants = new HashSet<>(utilisateurRepository.findAllById(dto.getParticipantIds()));
            reunion.setParticipants(participants);
        }

        Reunion saved = reunionRepository.save(reunion);
        notifierCreationMeeting(saved);

        return reunionMapper.toDto(saved);
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
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        return reunionMapper.toDto(reunion);
    }

    @Override
    public List<ReunionDto> getAll() {
        return reunionRepository.findAllByOrderByDateDescHeureDesc()
                .stream()
                .map(reunionMapper::toDto)
                .toList();
    }

    @Override
    public List<ReunionDto> getByStageId(Long stageId) {
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
        if (dto == null) {
            throw new IllegalArgumentException("Les donnees de la reunion sont obligatoires.");
        }

        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        getAuthorizedSupervisorForStage(reunion.getStage());
        ensureMeetingCanBeModified(reunion.getDate(), reunion.getHeure());

        if (dto.getNumReunion() != null && !dto.getNumReunion().isBlank()) {
            reunion.setNumReunion(dto.getNumReunion().trim());
        }
        reunion.setDate(dto.getDate());
        reunion.setHeure(dto.getHeure());
        reunion.setObservation(dto.getObservation());
        reunion.setCompteRendu(dto.getCompteRendu());

        if (dto.getStageId() != null) {
            Stage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + dto.getStageId()));
            getAuthorizedSupervisorForStage(stage);
            reunion.setStage(stage);
        }

        if (reunion.getStage() == null || reunion.getStage().getId() == null) {
            throw new IllegalArgumentException("Le stage est obligatoire");
        }

        validateActiveStage(reunion.getStage());
        validateMeetingWithinStagePeriod(reunion.getStage(), reunion.getDate());
        validateNoDuplicateMeeting(reunion.getStage().getId(), reunion.getDate(), reunion.getHeure(), reunion.getId());

        if (dto.getParticipantIds() != null) {
            Set<Utilisateur> participants = new HashSet<>(utilisateurRepository.findAllById(dto.getParticipantIds()));
            reunion.setParticipants(participants);
        }

        Reunion updated = reunionRepository.save(reunion);
        notifierMeetingUpdate(updated);
        return reunionMapper.toDto(updated);
    }

    @Override
    public ReunionDto ajouterCompteRendu(Long id, String compteRendu) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        getAuthorizedSupervisorForStage(reunion.getStage());

        reunion.setCompteRendu(compteRendu);
        Reunion updated = reunionRepository.save(reunion);
        notifierParticipants(updated, "Compte rendu de reunion mis a jour",
                "Le compte rendu de la reunion de suivi " + updated.getNumReunion() + " a ete mis a jour.");

        return reunionMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        Reunion reunion = reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reunion introuvable avec l'id : " + id));

        getAuthorizedSupervisorForStage(reunion.getStage());
        ensureMeetingCanBeCancelled(reunion.getDate(), reunion.getHeure());

        reunionRepository.delete(reunion);
        notifierMeetingCancellation(reunion);
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
            throw new IllegalArgumentException("Les dates du stage sont obligatoires pour planifier une reunion.");
        }

        if (meetingDate == null
                || meetingDate.isBefore(stage.getDateDebut())
                || meetingDate.isAfter(stage.getDateFin())) {
            throw new IllegalArgumentException("La reunion doit etre planifiee pendant la periode du stage.");
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
            throw new IllegalArgumentException("Une reunion existe deja pour ce stage a cette date et cette heure.");
        }
    }

    private void validateActiveStage(Stage stage) {
        if (stage == null || stage.getStatut() != StatutStage.EN_COURS) {
            throw new IllegalArgumentException("Le stage doit etre actif pour gerer une reunion de suivi.");
        }
    }

    private void ensureMeetingCanBeModified(LocalDate date, LocalTime heure) {
        if (date == null || heure == null) {
            throw new IllegalArgumentException("La date et l'heure de la reunion sont obligatoires.");
        }

        LocalDateTime meetingStart = LocalDateTime.of(date, heure);
        if (Duration.between(LocalDateTime.now(), meetingStart).compareTo(MIN_UPDATE_DELAY) < 0) {
            throw new IllegalArgumentException("Vous ne pouvez pas modifier une reunion moins de 24 heures avant son horaire.");
        }
    }

    private void ensureMeetingCanBeCancelled(LocalDate date, LocalTime heure) {
        if (date == null || heure == null) {
            throw new IllegalArgumentException("La date et l'heure de la reunion sont obligatoires.");
        }

        LocalDateTime meetingStart = LocalDateTime.of(date, heure);
        if (Duration.between(LocalDateTime.now(), meetingStart).compareTo(MIN_UPDATE_DELAY) < 0) {
            throw new IllegalArgumentException("Vous ne pouvez pas annuler une reunion moins de 24 heures avant son horaire.");
        }
    }

    private Utilisateur getAuthorizedSupervisorForStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associe a cette reunion.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        boolean academicSupervisor = utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                && stage.getEncadrantAcademique() != null
                && utilisateur.getId().equals(stage.getEncadrantAcademique().getId());
        boolean professionalSupervisor = utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage.getEncadrantProfessionnel() != null
                && utilisateur.getId().equals(stage.getEncadrantProfessionnel().getId());

        if (!academicSupervisor && !professionalSupervisor) {
            throw new AccessDeniedException("Seul un encadrant associe au stage peut gerer cette reunion de suivi.");
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

    private void notifierMeetingCancellation(Reunion reunion) {
        String message = "La reunion hebdomadaire " + safeText(reunion.getNumReunion(), "de suivi")
                + " prevue le " + reunion.getDate() + " a " + reunion.getHeure() + " a ete annulee.";
        notifierParticipants(reunion, "Reunion hebdomadaire annulee", message);
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
}
