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
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReunionFinaleServiceImpl implements ReunionFinaleService {

    private final ReunionFinaleRepository reunionFinaleRepository;
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

        List<ReunionFinaleDto> reunions = reunionFinaleRepository.findByStageId(stageId)
                .stream()
                .map(reunionFinaleMapper::toDto)
                .toList();

        log.info("{} reunion(s) finale(s) chargee(s) pour le stage {}", reunions.size(), stageId);
        return reunions;
    }

    @Override
    public ReunionFinaleDto update(Long id, ReunionFinaleDto dto) {
        ReunionFinale entity = reunionFinaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion finale introuvable avec l'id : " + id));

        authorizeFinalMeetingManagement(entity.getStage());

        Stage stage = entity.getStage();
        if (stage == null) {
            throw new RuntimeException("Aucun stage n'est associe a cette reunion finale");
        }

        if (dto != null) {
            if (dto.getStageId() != null && !dto.getStageId().equals(stage.getId())) {
                throw new AccessDeniedException("Le stage associe a une reunion finale automatique ne peut pas etre modifie.");
            }
            if (dto.getDate() != null && !dto.getDate().equals(entity.getDate())) {
                throw new AccessDeniedException("La date de la reunion finale est geree automatiquement par le systeme.");
            }
            if (dto.getHeure() != null && !dto.getHeure().equals(entity.getHeure())) {
                throw new AccessDeniedException("L'heure de la reunion finale est geree automatiquement par le systeme.");
            }
            if (dto.getNumReunion() != null && !dto.getNumReunion().equals(entity.getNumReunion())) {
                throw new AccessDeniedException("Le numero de la reunion finale est gere automatiquement par le systeme.");
            }
        }

        entity.setObservation(dto != null ? dto.getObservation() : entity.getObservation());
        entity.setCompteRendu(dto != null ? dto.getCompteRendu() : entity.getCompteRendu());
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
