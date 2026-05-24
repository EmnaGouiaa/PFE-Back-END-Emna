package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;

import java.util.List;

public interface ReunionFinaleService {

    ReunionFinaleDto create(ReunionFinaleDto dto);

    ReunionFinaleDto getById(Long id);

    List<ReunionFinaleDto> getAll();

    List<ReunionFinaleDto> getByStageId(Long stageId);

    ReunionFinaleDto update(Long id, ReunionFinaleDto dto);

    void delete(Long id);
}
