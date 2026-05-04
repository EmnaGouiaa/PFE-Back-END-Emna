package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;

import java.util.List;

public interface EncadrantAcademiqueService {

    EncadrantAcademiqueDto create(EncadrantAcademiqueDto dto);

    EncadrantAcademiqueDto update(Long id, EncadrantAcademiqueDto dto);

    EncadrantAcademiqueDto getById(Long id);

    List<EncadrantAcademiqueDto> getAll();

    void delete(Long id);
}