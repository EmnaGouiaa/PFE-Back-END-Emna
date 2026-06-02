package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionFinaleMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.service.FinalMeetingUpdatePolicy;
import fsegs.pfebackendemnagouuiaa.service.MeetingVisibilityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReunionFinaleServiceImpl implements ReunionFinaleService {

    private static final String ERROR_NOT_PARTICIPANT = "Accès refusé : vous n'êtes pas participant à cette réunion.";

    private final ReunionFinaleRepository reunionFinaleRepository;
    private final ReunionRepository reunionRepository;
    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final ReunionFinaleMapper reunionFinaleMapper;
    private final JwtService jwtService;

    @Override
    public ReunionFinaleDto create(ReunionFinaleDto dto) {
        throw new AccessDeniedException("La reunion finale est creee automatiquement par le systeme.");
    }

    @Override
    public ReunionFinaleDto getById(Long id) {
        ReunionFinale entity = reunionFinaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion finale introuvable avec l'id : " + id));

        authorizeStageAccess(entity.getStage());
        ensureSupervisorParticipantIfApplicable(entity);
        log.info("Consultation de la reunion finale id={}", id);
        return reunionFinaleMapper.toDto(entity);
    }

    @Override
    public List<ReunionFinaleDto> getAll() {
        List<ReunionFinaleDto> reunions = reunionFinaleRepository.findAll()
                .stream()
                .map(reunionFinaleMapper::toDto)
                .toList();

        log.info("{} reunion(s) finale(s) chargee(s)", reunions.size());
        return reunions;
    }

    @Override
    public List<ReunionFinaleDto> getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + stageId));

        authorizeStageAccess(stage);

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur().orElse(null);
        List<ReunionFinale> entities = reunionFinaleRepository.findByStageId(stageId);
        if (MeetingVisibilityRules.isSupervisor(utilisateur) && utilisateur.getId() != null) {
            entities = entities.stream()
                    .filter(entity -> MeetingVisibilityRules.canSupervisorListFinalMeeting(utilisateur, entity, stage))
                    .toList();
        }

        List<ReunionFinaleDto> reunions = entities.stream()
                .map(reunionFinaleMapper::toDto)
                .toList();

        log.info("{} reunion(s) finale(s) chargee(s) pour le stage {}", reunions.size(), stageId);
        return reunions;
    }

    @Override
    public ReunionFinaleDto update(Long id, ReunionFinaleDto dto) {
        ReunionFinale entity = reunionFinaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion finale introuvable avec l'id : " + id));

        Utilisateur supervisor = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
        authorizeFinalMeetingManagement(entity.getStage());
        ensureSupervisorIsMeetingParticipant(entity, supervisor);

        Stage stage = entity.getStage();
        if (stage == null) {
            throw new RuntimeException("Aucun stage n'est associe a cette reunion finale");
        }

        FinalMeetingUpdatePolicy.assertLockedFieldsUnchanged(
                entity,
                dto != null ? dto.getStageId() : null,
                dto != null ? dto.getNumReunion() : null,
                dto != null ? dto.getCompteRendu() : null,
                dto != null ? dto.getParticipantIds() : null
        );
        FinalMeetingUpdatePolicy.assertOnlyHeureMayChange(
                entity,
                dto != null ? dto.getDate() : null,
                dto != null ? dto.getHeure() : null,
                dto != null ? dto.getObservation() : null
        );

        entity.setHeure(dto.getHeure());
        validateMeetingWithinStagePeriod(stage, entity.getDate());
        validateNoDuplicateMeeting(stage.getId(), entity.getDate(), entity.getHeure(), entity.getId());
        entity.setCahierStage(resolveCahierStage(stage));

        ReunionFinale updated = reunionFinaleRepository.save(entity);
        log.info("Reunion finale mise a jour. id={}", updated.getId());
        return reunionFinaleMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        throw new AccessDeniedException("La suppression manuelle de la reunion finale est interdite.");
    }

    private void authorizeFinalMeetingManagement(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associe a cette reunion finale.");
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
            throw new AccessDeniedException("Seul un encadrant associe au stage peut gerer cette reunion finale.");
        }
    }

    private void authorizeStageAccess(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associe a cette reunion finale.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        if (isManagementRole(utilisateur.getRole())) {
            return;
        }

        if (!isConcernedActor(stage, utilisateur)) {
            throw new AccessDeniedException("Acces refuse a une reunion finale qui n'est pas liee a votre stage.");
        }
    }

    private boolean isConcernedActor(Stage stage, Utilisateur utilisateur) {
        if (utilisateur.getRole() == Role.STAGIAIRE) {
            return isSameUser(stage.getStagiaire(), utilisateur);
        }
        if (utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE) {
            return isSameUser(stage.getEncadrantAcademique(), utilisateur);
        }
        if (utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL) {
            return isSameUser(stage.getEncadrantProfessionnel(), utilisateur);
        }
        if (utilisateur.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            if (!(utilisateur instanceof ResponsableEntreprise re)) return false;
            return re.getEntreprise() != null
                    && stage.getEntreprise() != null
                    && re.getEntreprise().getId().equals(stage.getEntreprise().getId());
        }
        return false;
    }

    private boolean isSameUser(Utilisateur expected, Utilisateur actual) {
        return expected != null
                && expected.getId() != null
                && actual != null
                && expected.getId().equals(actual.getId());
    }

    private boolean isManagementRole(Role role) {
        return role == Role.ADMINISTRATEUR || role == Role.RESPONSABLE_STAGE;
    }

    private boolean isSupervisor(Utilisateur utilisateur) {
        return utilisateur != null
                && (utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                || utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL);
    }

    private boolean isMeetingParticipant(ReunionFinale reunion, Utilisateur utilisateur) {
        if (reunion == null || utilisateur == null || utilisateur.getId() == null) {
            return false;
        }
        Set<Utilisateur> participants = reunion.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return false;
        }
        return participants.stream()
                .anyMatch(participant -> participant != null
                        && Objects.equals(participant.getId(), utilisateur.getId()));
    }

    private void ensureSupervisorIsMeetingParticipant(ReunionFinale reunion, Utilisateur supervisor) {
        if (!isSupervisor(supervisor)) {
            return;
        }
        if (!isMeetingParticipant(reunion, supervisor)) {
            throw new AccessDeniedException(ERROR_NOT_PARTICIPANT);
        }
    }

    private void ensureSupervisorParticipantIfApplicable(ReunionFinale reunion) {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur().orElse(null);
        if (!isSupervisor(utilisateur)) {
            return;
        }
        ensureSupervisorIsMeetingParticipant(reunion, utilisateur);
    }

    private void validateMeetingWithinStagePeriod(Stage stage, LocalDate meetingDate) {
        LocalDate dateDebut = stage != null ? stage.getDateDebut() : null;
        LocalDate dateFin = resolveStageEndDate(stage);
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Les dates du stage sont requises pour planifier la réunion finale.");
        }
        if (meetingDate == null
                || meetingDate.isBefore(dateDebut)
                || meetingDate.isAfter(dateFin)) {
            throw new IllegalArgumentException("La date de la réunion finale doit être comprise dans la période du stage.");
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

    private void validateNoDuplicateMeeting(Long stageId, LocalDate date, LocalTime heure, Long currentMeetingId) {
        if (stageId == null || date == null || heure == null) {
            return;
        }
        boolean exists = currentMeetingId == null
                ? reunionRepository.existsByStageIdAndDateAndHeure(stageId, date, heure)
                : reunionRepository.existsByStageIdAndDateAndHeureAndIdNot(stageId, date, heure, currentMeetingId);
        if (exists) {
            throw new IllegalArgumentException("Une réunion existe déjà à cette date et heure pour ce stage.");
        }
    }

    private CahierStage resolveCahierStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return null;
        }
        if (stage.getCahierStage() != null) {
            return stage.getCahierStage();
        }
        return cahierStageRepository.findByStageId(stage.getId())
                .map(cahierStage -> {
                    stage.setCahierStage(cahierStage);
                    return cahierStage;
                })
                .orElse(null);
    }
}
