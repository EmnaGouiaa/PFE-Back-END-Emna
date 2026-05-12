package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionHebdomadaireMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionHebdomadaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReunionHebdomadaireServiceImpl implements ReunionHebdomadaireService {

    private final ReunionHebdomadaireRepository reunionHebdomadaireRepository;
    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReunionHebdomadaireMapper reunionHebdomadaireMapper;
    private final JwtService jwtService;

    @Override
    public ReunionHebdomadaireDto create(ReunionHebdomadaireDto dto) {
        ReunionHebdomadaire entity = reunionHebdomadaireMapper.toEntity(dto);

        if (dto.getStageId() == null) {
            throw new RuntimeException("Le stage est obligatoire");
        }

        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));
        entity.setStage(stage);
        validateMeetingWithinStagePeriod(stage, dto.getDate());
        validateNoDuplicateMeeting(stage.getId(), dto.getDate(), dto.getHeure(), null);
        entity.setCahierStage(resolveOrCreateCahierStage(stage));

        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            Set<Utilisateur> participants = new HashSet<>(utilisateurRepository.findAllById(dto.getParticipantIds()));
            entity.setParticipants(participants);
        }

        ReunionHebdomadaire saved = reunionHebdomadaireRepository.save(entity);
        return reunionHebdomadaireMapper.toDto(saved);
    }

    @Override
    public ReunionHebdomadaireDto getById(Long id) {
        ReunionHebdomadaire entity = reunionHebdomadaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion hebdomadaire introuvable avec l'id : " + id));

        authorizeStageAccess(entity.getStage());
        return reunionHebdomadaireMapper.toDto(entity);
    }

    @Override
    public List<ReunionHebdomadaireDto> getAll() {
        return reunionHebdomadaireRepository.findAll()
                .stream()
                .map(reunionHebdomadaireMapper::toDto)
                .toList();
    }

    @Override
    public List<ReunionHebdomadaireDto> getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + stageId));

        authorizeStageAccess(stage);

        return reunionHebdomadaireRepository.findByStageId(stageId)
                .stream()
                .map(reunionHebdomadaireMapper::toDto)
                .toList();
    }

    @Override
    public List<ReunionHebdomadaireDto> getByCahierStageId(Long cahierStageId) {
        return reunionHebdomadaireRepository.findByCahierStageId(cahierStageId)
                .stream()
                .map(reunionHebdomadaireMapper::toDto)
                .toList();
    }

    @Override
    public ReunionHebdomadaireDto update(Long id, ReunionHebdomadaireDto dto) {
        ReunionHebdomadaire entity = reunionHebdomadaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion hebdomadaire introuvable avec l'id : " + id));

        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());
        entity.setNumSemaine(dto.getNumSemaine());
        entity.setObjectifsSemaineProchaine(dto.getObjectifsSemaineProchaine());

        if (dto.getStageId() != null) {
            Stage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));
            entity.setStage(stage);
        }

        Stage stage = entity.getStage();
        if (stage == null || stage.getId() == null) {
            throw new RuntimeException("Le stage est obligatoire");
        }

        validateMeetingWithinStagePeriod(stage, entity.getDate());
        validateNoDuplicateMeeting(stage.getId(), entity.getDate(), entity.getHeure(), entity.getId());

        entity.setCahierStage(resolveOrCreateCahierStage(stage));

        if (dto.getParticipantIds() != null) {
            Set<Utilisateur> participants = new HashSet<>(utilisateurRepository.findAllById(dto.getParticipantIds()));
            entity.setParticipants(participants);
        }

        ReunionHebdomadaire updated = reunionHebdomadaireRepository.save(entity);
        return reunionHebdomadaireMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        if (!reunionHebdomadaireRepository.existsById(id)) {
            throw new RuntimeException("Reunion hebdomadaire introuvable avec l'id : " + id);
        }

        throw new AccessDeniedException("La suppression d'une reunion de suivi est interdite.");
    }

    private void validateMeetingWithinStagePeriod(Stage stage, LocalDate meetingDate) {
        if (stage.getDateDebut() == null || stage.getDateFin() == null) {
            throw new RuntimeException("Les dates du stage sont obligatoires pour planifier une reunion.");
        }

        if (meetingDate == null
                || meetingDate.isBefore(stage.getDateDebut())
                || meetingDate.isAfter(stage.getDateFin())) {
            throw new RuntimeException("La reunion hebdomadaire doit etre planifiee pendant la periode du stage.");
        }
    }

    private void validateNoDuplicateMeeting(Long stageId, LocalDate date, LocalTime heure, Long currentMeetingId) {
        if (stageId == null || date == null || heure == null) {
            return;
        }

        boolean exists = currentMeetingId == null
                ? reunionHebdomadaireRepository.existsByStageIdAndDateAndHeure(stageId, date, heure)
                : reunionHebdomadaireRepository.existsByStageIdAndDateAndHeureAndIdNot(stageId, date, heure, currentMeetingId);

        if (exists) {
            throw new RuntimeException("Une reunion existe deja pour ce stage a cette date et cette heure.");
        }
    }

    private void authorizeStageAccess(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associe a cette reunion.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        if (utilisateur.getRole() == Role.STAGIAIRE) {
            Long stageOwnerId = stage.getStagiaire() != null ? stage.getStagiaire().getId() : null;
            if (stageOwnerId == null || !stageOwnerId.equals(utilisateur.getId())) {
                throw new AccessDeniedException("Acces refuse a une reunion qui n'est pas liee a votre stage.");
            }
        }
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
