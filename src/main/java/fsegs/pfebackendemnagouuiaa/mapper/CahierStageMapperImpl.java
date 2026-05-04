package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.mapper.CahierStageMapper;
import org.springframework.stereotype.Component;

@Component
public class CahierStageMapperImpl implements CahierStageMapper {

    @Override
    public CahierStageDto toDto(CahierStage entity) {
        if (entity == null) {
            return null;
        }

        CahierStageDto dto = new CahierStageDto();
        dto.setId(entity.getId());
        dto.setDateGeneration(entity.getDateGeneration());
        dto.setDateSignature(entity.getDateSignature());
        dto.setEstSigne(entity.getEstSigne());
        dto.setSigneeEncAcad(entity.getSigneeEncAcad());
        dto.setSigneeEncPro(entity.getSigneeEncPro());
        dto.setSigneeRespEntreprise(entity.getSigneeRespEntreprise());
        dto.setSigneeStagiaire(entity.getSigneeStagiaire());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        return dto;
    }

    @Override
    public CahierStage toEntity(CahierStageDto dto) {
        if (dto == null) {
            return null;
        }

        CahierStage entity = new CahierStage();
        entity.setId(dto.getId());
        entity.setDateGeneration(dto.getDateGeneration());
        entity.setDateSignature(dto.getDateSignature());
        entity.setEstSigne(dto.getEstSigne());
        entity.setSigneeEncAcad(dto.getSigneeEncAcad());
        entity.setSigneeEncPro(dto.getSigneeEncPro());
        entity.setSigneeRespEntreprise(dto.getSigneeRespEntreprise());
        entity.setSigneeStagiaire(dto.getSigneeStagiaire());

        return entity;
    }
}