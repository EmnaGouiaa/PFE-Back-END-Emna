package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.AbsenceMapper;
import fsegs.pfebackendemnagouuiaa.repository.AbsenceRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final StageRepository stageRepository;
    private final AbsenceMapper absenceMapper;
    private final JwtService jwtService;

    @Override
    public AbsenceDto create(AbsenceDto dto) {
        Absence entity = absenceMapper.toEntity(dto);

        if (dto.getStageId() == null) {
            throw new RuntimeException("Le stage est obligatoire");
        }

        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));
        ensureResponsableEntrepriseScope(stage);
        validateAbsence(stage, dto);

        entity.setStage(stage);
        entity.setNbAbsence(dto.getNbAbsence() == null || dto.getNbAbsence() <= 0 ? 1 : dto.getNbAbsence());
        entity.setJustification(normalizeRequiredText(dto.getJustification(), "La justification de l'absence est obligatoire."));
        entity.setCommentaire(normalizeOptionalText(dto.getCommentaire()));
        entity.setStatut(normalizeOptionalText(dto.getStatut()));

        Absence saved = absenceRepository.save(entity);
        return absenceMapper.toDto(saved);
    }

    @Override
    public AbsenceDto getById(Long id) {
        Absence entity = absenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Absence introuvable avec l'id : " + id));

        return absenceMapper.toDto(entity);
    }

    @Override
    public List<AbsenceDto> getAll() {
        return absenceRepository.findAll()
                .stream()
                .map(absenceMapper::toDto)
                .toList();
    }

    @Override
    public List<AbsenceDto> getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + stageId));
        ensureCanReadStageAbsences(stage);
        return absenceRepository.findByStageId(stageId)
                .stream()
                .map(absenceMapper::toDto)
                .toList();
    }

    @Override
    public AbsenceDto update(Long id, AbsenceDto dto) {
        Absence entity = absenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Absence introuvable avec l'id : " + id));
        Stage stage = entity.getStage();
        if (dto.getStageId() != null && (stage == null || !dto.getStageId().equals(stage.getId()))) {
            stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));
        }
        ensureResponsableEntrepriseScope(stage);
        validateAbsence(stage, dto);

        entity.setDateAbsence(dto.getDateAbsence());
        entity.setNbAbsence(dto.getNbAbsence() == null || dto.getNbAbsence() <= 0 ? 1 : dto.getNbAbsence());
        entity.setJustification(normalizeRequiredText(dto.getJustification(), "La justification de l'absence est obligatoire."));
        entity.setCommentaire(normalizeOptionalText(dto.getCommentaire()));
        entity.setStatut(normalizeOptionalText(dto.getStatut()));
        entity.setStage(stage);

        Absence updated = absenceRepository.save(entity);
        return absenceMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        Absence entity = absenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Absence introuvable avec l'id : " + id));
        ensureResponsableEntrepriseScope(entity.getStage());

        absenceRepository.delete(entity);
    }

    private void validateAbsence(Stage stage, AbsenceDto dto) {
        if (stage == null) {
            throw new RuntimeException("Le stage associe a cette absence est introuvable.");
        }
        if (dto.getDateAbsence() == null) {
            throw new RuntimeException("La date de l'absence est obligatoire.");
        }
        LocalDate dateDebut = stage.getDateDebut();
        LocalDate dateFin = stage.getDateFin();
        if (dateDebut != null && dto.getDateAbsence().isBefore(dateDebut)
                || dateFin != null && dto.getDateAbsence().isAfter(dateFin)) {
            throw new RuntimeException("L'absence doit etre enregistree pendant la periode du stage.");
        }
    }

    private void ensureResponsableEntrepriseScope(Stage stage) {
        Utilisateur utilisateur = getAuthenticatedUser();
        if (utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Seul le representant entreprise peut gerer les absences.");
        }
        if (!(utilisateur instanceof ResponsableEntreprise responsableEntreprise)) {
            throw new AccessDeniedException("Compte responsable entreprise introuvable.");
        }

        Long expectedEntrepriseId = responsableEntreprise.getEntreprise() != null ? responsableEntreprise.getEntreprise().getId() : null;
        Long stageEntrepriseId = stage != null && stage.getEntreprise() != null ? stage.getEntreprise().getId() : null;
        if (expectedEntrepriseId == null || stageEntrepriseId == null || !expectedEntrepriseId.equals(stageEntrepriseId)) {
            throw new AccessDeniedException("Vous ne pouvez gerer les absences que pour les stages de votre entreprise.");
        }
    }

    private void ensureCanReadStageAbsences(Stage stage) {
        Utilisateur utilisateur = getAuthenticatedUser();
        Long userId = utilisateur.getId();
        boolean allowed = switch (utilisateur.getRole()) {
            case ADMINISTRATEUR, RESPONSABLE_SERVICE_STAGES, RESPONSABLE_UNIVERSITAIRE_STAGES -> true;
            case STAGIAIRE -> stage.getStagiaire() != null && userId.equals(stage.getStagiaire().getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && userId.equals(stage.getEncadrantAcademique().getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && userId.equals(stage.getEncadrantProfessionnel().getId());
            case RESPONSABLE_ENTREPRISE -> stage.getEntreprise() != null
                    && utilisateur instanceof ResponsableEntreprise responsableEntreprise
                    && responsableEntreprise.getEntreprise() != null
                    && stage.getEntreprise().getId().equals(responsableEntreprise.getEntreprise().getId());
            default -> false;
        };

        if (!allowed) {
            throw new AccessDeniedException("Acces refuse a la feuille d'absences de ce stage.");
        }
    }

    private Utilisateur getAuthenticatedUser() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
