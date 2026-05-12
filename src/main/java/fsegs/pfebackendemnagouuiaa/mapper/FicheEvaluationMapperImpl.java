package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.repository.NoteAttribueeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FicheEvaluationMapperImpl implements FicheEvaluationMapper {

    private final NoteAttribueeRepository noteAttribueeRepository;
    private final NoteAttribueeMapper noteAttribueeMapper;

    @Override
    public FicheEvaluationDto toDto(FicheEvaluation entity) {
        if (entity == null) {
            return null;
        }

        FicheEvaluationDto dto = new FicheEvaluationDto();

        dto.setId(entity.getId());

        dto.setPointFortEncadrantPro(entity.getPointFortEncadrantPro());
        dto.setAxeAmeliorationEncadrantPro(entity.getAxeAmeliorationEncadrantPro());
        dto.setSignatureEncadrantProfessionnel(entity.getSignatureEncadrantProfessionnel());
        dto.setDateSignatureEncadrantProfessionnel(entity.getDateSignatureEncadrantProfessionnel());
        dto.setSignataireEncadrantProfessionnelId(entity.getSignataireEncadrantProfessionnelId());
        dto.setRoleSignatureEncadrantProfessionnel(entity.getRoleSignatureEncadrantProfessionnel());
        dto.setNomSignataireEncadrantProfessionnel(entity.getNomSignataireEncadrantProfessionnel());

        dto.setPointFortResponsableEntreprise(entity.getPointFortResponsableEntreprise());
        dto.setAxeAmeliorationResponsableEntreprise(entity.getAxeAmeliorationResponsableEntreprise());
        dto.setSignatureRepresentantEntreprise(entity.getSignatureRepresentantEntreprise());
        dto.setDateSignatureRepresentantEntreprise(entity.getDateSignatureRepresentantEntreprise());
        dto.setSignataireRepresentantEntrepriseId(entity.getSignataireRepresentantEntrepriseId());
        dto.setRoleSignatureRepresentantEntreprise(entity.getRoleSignatureRepresentantEntreprise());
        dto.setNomSignataireRepresentantEntreprise(entity.getNomSignataireRepresentantEntreprise());

        dto.setNoteFinale(entity.getNoteFinale());

        if (entity.getStage() != null) {
            Stage stage = entity.getStage();
            dto.setStageId(stage.getId());
            dto.setStageTitre(stage.getTitre());
            dto.setStageSujet(stage.getSujet());
            dto.setStageDateDebut(stage.getDateDebut());
            dto.setStageDateFin(stage.getDateFin());
            dto.setStagiaireNomComplet(stage.getStagiaire() == null
                    ? ""
                    : ((stage.getStagiaire().getPrenom() == null ? "" : stage.getStagiaire().getPrenom().trim()) + " "
                    + (stage.getStagiaire().getNom() == null ? "" : stage.getStagiaire().getNom().trim())).trim());
            dto.setSectionStagiaire(stage.getStagiaire() == null || stage.getStagiaire().getFiliere() == null
                    ? ""
                    : stage.getStagiaire().getFiliere().getNom());
            dto.setEntrepriseNom(stage.getEntreprise() == null ? "" : stage.getEntreprise().getNom());
            dto.setEntrepriseLieuStage(stage.getEntreprise() == null ? "" : stage.getEntreprise().getAdresse());
        }

        if (entity.getReunionFinale() != null) {
            dto.setReunionFinaleId(entity.getReunionFinale().getId());
            dto.setReunionFinaleNumero(entity.getReunionFinale().getNumReunion());
            dto.setReunionFinaleDate(entity.getReunionFinale().getDate());
            dto.setReunionFinaleHeure(entity.getReunionFinale().getHeure());
        }

        dto.setDonneesCompletes(entity.donneesCompletes());
        dto.setComplete(entity.donneesCompletes());
        dto.setSignaturesCompletes(entity.signaturesCompletes());
        dto.setVerrouillee(entity.estVerrouillee());
        dto.setNotesAttribuees(loadNotes(entity.getId()));

        return dto;
    }

    @Override
    public FicheEvaluation toEntity(FicheEvaluationDto dto) {
        if (dto == null) {
            return null;
        }

        FicheEvaluation entity = new FicheEvaluation();

        entity.setId(dto.getId());

        entity.setPointFortEncadrantPro(dto.getPointFortEncadrantPro());
        entity.setAxeAmeliorationEncadrantPro(dto.getAxeAmeliorationEncadrantPro());
        entity.setSignatureEncadrantProfessionnel(dto.getSignatureEncadrantProfessionnel());
        entity.setDateSignatureEncadrantProfessionnel(dto.getDateSignatureEncadrantProfessionnel());
        entity.setSignataireEncadrantProfessionnelId(dto.getSignataireEncadrantProfessionnelId());
        entity.setRoleSignatureEncadrantProfessionnel(dto.getRoleSignatureEncadrantProfessionnel());
        entity.setNomSignataireEncadrantProfessionnel(dto.getNomSignataireEncadrantProfessionnel());

        entity.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise());
        entity.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise());
        entity.setSignatureRepresentantEntreprise(dto.getSignatureRepresentantEntreprise());
        entity.setDateSignatureRepresentantEntreprise(dto.getDateSignatureRepresentantEntreprise());
        entity.setSignataireRepresentantEntrepriseId(dto.getSignataireRepresentantEntrepriseId());
        entity.setRoleSignatureRepresentantEntreprise(dto.getRoleSignatureRepresentantEntreprise());
        entity.setNomSignataireRepresentantEntreprise(dto.getNomSignataireRepresentantEntreprise());

        entity.setNoteFinale(dto.getNoteFinale());

        if (dto.getStageId() != null) {
            Stage stage = new Stage();
            stage.setId(dto.getStageId());
            entity.setStage(stage);
        }

        if (dto.getReunionFinaleId() != null) {
            ReunionFinale reunionFinale = new ReunionFinale();
            reunionFinale.setId(dto.getReunionFinaleId());
            entity.setReunionFinale(reunionFinale);
        }

        return entity;
    }

    private List<NoteAttribueeDto> loadNotes(Long ficheId) {
        if (ficheId == null) {
            return List.of();
        }

        return noteAttribueeRepository.findByFicheEvaluationId(ficheId).stream()
                .sorted(Comparator.comparing(note -> note.getCritereEvaluation() == null
                        ? ""
                        : String.valueOf(note.getCritereEvaluation().getLibelle()), String.CASE_INSENSITIVE_ORDER))
                .map(noteAttribueeMapper::toDto)
                .toList();
    }
}
