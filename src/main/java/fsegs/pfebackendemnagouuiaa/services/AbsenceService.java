package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;

import java.util.List;

public interface AbsenceService {

    AbsenceDto create(AbsenceDto dto);

    AbsenceDto getById(Long id);

    List<AbsenceDto> getAll();

    List<AbsenceDto> getByStageId(Long stageId);

    AbsenceDto update(Long id, AbsenceDto dto);

    void delete(Long id);
}