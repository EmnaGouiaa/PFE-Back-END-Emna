package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.mapper.ConventionStageMapper;
import org.springframework.stereotype.Component;

@Component
public class ConventionStageMapperImpl implements ConventionStageMapper {

    @Override
    public ConventionStageDto toDto(ConventionStage entity) {
        if (entity == null) {
            return null;
        }

        ConventionStageDto dto = new ConventionStageDto();
        dto.setId(entity.getId());
        dto.setNumConv(entity.getNumConv());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setSigneeEncAca(entity.getSigneeEncAca());
        dto.setSigneeEncPro(entity.getSigneeEncPro());
        dto.setSigneeEntreprise(entity.getSigneeEntreprise());
        dto.setSigneeResp(entity.getSigneeResp());
        dto.setDateSignatureResponsableUniversitaire(entity.getDateSignatureResponsableUniversitaire());
        dto.setNomResponsableUniversitaireSignataire(entity.getNomResponsableUniversitaireSignataire());
        dto.setSigneeStagiaire(entity.getSigneeStagiaire());
        dto.setStatutSignatures(entity.getStatutSignatures());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        if (entity.getDemandeStage() != null) {
            dto.setDemandeStageId(entity.getDemandeStage().getId());
        }

        return dto;
    }

    @Override
    public ConventionStage toEntity(ConventionStageDto dto) {
        if (dto == null) {
            return null;
        }

        ConventionStage entity = new ConventionStage();
        entity.setId(dto.getId());
        entity.setNumConv(dto.getNumConv());
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
        entity.setSigneeEncAca(dto.getSigneeEncAca());
        entity.setSigneeEncPro(dto.getSigneeEncPro());
        entity.setSigneeEntreprise(dto.getSigneeEntreprise());
        entity.setSigneeResp(dto.getSigneeResp());
        entity.setDateSignatureResponsableUniversitaire(dto.getDateSignatureResponsableUniversitaire());
        entity.setNomResponsableUniversitaireSignataire(dto.getNomResponsableUniversitaireSignataire());
        entity.setSigneeStagiaire(dto.getSigneeStagiaire());
        entity.setStatutSignatures(dto.getStatutSignatures());

        return entity;
    }
}
