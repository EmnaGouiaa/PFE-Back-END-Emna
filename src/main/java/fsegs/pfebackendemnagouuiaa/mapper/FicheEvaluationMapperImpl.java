package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import org.springframework.stereotype.Component;

@Component
public class FicheEvaluationMapperImpl implements FicheEvaluationMapper {

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

        dto.setPointFortResponsableEntreprise(entity.getPointFortResponsableEntreprise());
        dto.setAxeAmeliorationResponsableEntreprise(entity.getAxeAmeliorationResponsableEntreprise());
        dto.setSignatureRepresentantEntreprise(entity.getSignatureRepresentantEntreprise());
        dto.setDateSignatureRepresentantEntreprise(entity.getDateSignatureRepresentantEntreprise());

        dto.setNoteFinale(entity.getNoteFinale());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        if (entity.getReunionFinale() != null) {
            dto.setReunionFinaleId(entity.getReunionFinale().getId());
        }

        dto.setDonneesCompletes(entity.donneesCompletes());
        dto.setComplete(entity.donneesCompletes());
        dto.setSignaturesCompletes(entity.signaturesCompletes());
        dto.setVerrouillee(entity.estVerrouillee());

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

        entity.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise());
        entity.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise());
        entity.setSignatureRepresentantEntreprise(dto.getSignatureRepresentantEntreprise());
        entity.setDateSignatureRepresentantEntreprise(dto.getDateSignatureRepresentantEntreprise());

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
}
