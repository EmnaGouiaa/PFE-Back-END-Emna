package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleFormulairesUpdateRequest;

import java.util.List;

public interface ReunionFinaleService {

    ReunionFinaleDto create(ReunionFinaleDto dto);

    ReunionFinaleDto getById(Long id);

    List<ReunionFinaleDto> getAll();

    List<ReunionFinaleDto> getByStageId(Long stageId);

    ReunionFinaleDto update(Long id, ReunionFinaleDto dto);

    ReunionFinaleDto updateFormulaires(Long id, ReunionFinaleFormulairesUpdateRequest request);

    EnqueteSatisfactionDto getEnqueteSatisfaction(Long reunionFinaleId);

    void delete(Long id);
}
