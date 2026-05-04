package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;

import java.util.List;

public interface ConventionStageService {

    ConventionStageDto create(ConventionStageDto dto);

    ConventionStageDto createByStage(Long stageId, ConventionStageDto dto);

    ConventionStageDto getById(Long id);

    ConventionStageDto getByStageId(Long stageId);

    List<ConventionStageDto> getAll();

    ConventionStageDto update(Long id, ConventionStageDto dto);

    ConventionStageDto signerParStagiaire(Long id);

    ConventionStageDto signerParEncadrantAcademique(Long id);

    ConventionStageDto signerParEncadrantProfessionnel(Long id);

    ConventionStageDto signerParEntreprise(Long id);

    ConventionStageDto signerParResponsable(Long id);

    void delete(Long id);

}