package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.dto.SignerCahierRequest;

import java.util.List;
import java.util.Optional;

public interface CahierStageService {

    CahierStageDto create(CahierStageDto dto);

    CahierStageDto createByStage(Long stageId, CahierStageDto dto);

    CahierStageDto getById(Long id);

    Optional<CahierStageDto> findByStageIdIfPresent(Long stageId);

    CahierStageDto getByStageId(Long stageId);

    List<CahierStageDto> getAll();

    CahierStageDto update(Long id, CahierStageDto dto);

    CahierStageDto signerParStagiaire(Long id, SignerCahierRequest request);

    CahierStageDto signerParEncadrantAcademique(Long id, SignerCahierRequest request);

    CahierStageDto signerParEncadrantProfessionnel(Long id, SignerCahierRequest request);

    CahierStageDto signerParResponsableEntreprise(Long id, SignerCahierRequest request);

    void delete(Long id);
}