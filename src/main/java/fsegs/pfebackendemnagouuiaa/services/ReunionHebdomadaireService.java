package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;

import java.util.List;

public interface ReunionHebdomadaireService {

    ReunionHebdomadaireDto create(ReunionHebdomadaireDto dto);

    ReunionHebdomadaireDto getById(Long id);

    List<ReunionHebdomadaireDto> getAll();

    List<ReunionHebdomadaireDto> getByStageId(Long stageId);

    List<ReunionHebdomadaireDto> getByCahierStageId(Long cahierStageId);

    ReunionHebdomadaireDto update(Long id, ReunionHebdomadaireDto dto);

    void delete(Long id);
}