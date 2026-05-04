package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;

import java.util.List;

public interface ReunionService {

    ReunionDto create(ReunionDto dto);

    ReunionDto getById(Long id);

    List<ReunionDto> getAll();

    List<ReunionDto> getByStageId(Long stageId);

    List<ReunionDto> getPourStagiaireAuthentifie();

    List<ReunionDto> getPourEntrepriseAuthentifiee();

    ReunionDto update(Long id, ReunionDto dto);

    ReunionDto ajouterCompteRendu(Long id, String compteRendu);

    void delete(Long id);
}
