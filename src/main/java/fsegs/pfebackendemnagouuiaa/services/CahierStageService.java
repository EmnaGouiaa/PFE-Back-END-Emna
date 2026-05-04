package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;

import java.util.List;

public interface CahierStageService {

    CahierStageDto create(CahierStageDto dto);

    CahierStageDto createByStage(Long stageId, CahierStageDto dto);

    CahierStageDto getById(Long id);

    CahierStageDto getByStageId(Long stageId);

    List<CahierStageDto> getAll();

    CahierStageDto update(Long id, CahierStageDto dto);

    CahierStageDto signerParStagiaire(Long id);

    CahierStageDto signerParEncadrantAcademique(Long id);

    CahierStageDto signerParEncadrantProfessionnel(Long id);

    CahierStageDto signerParResponsableEntreprise(Long id);

    void delete(Long id);
}