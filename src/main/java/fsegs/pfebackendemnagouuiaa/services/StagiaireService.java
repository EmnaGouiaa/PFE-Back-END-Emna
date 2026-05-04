package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AffectationEncadrantAcademiqueResponse;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireResponseDTO;

import java.util.List;

public interface StagiaireService {
    StagiaireResponseDTO create(StagiaireRequestDTO dto);

    StagiaireResponseDTO update(Long id, StagiaireRequestDTO dto);

    StagiaireResponseDTO getById(Long id);

    List<StagiaireResponseDTO> getAll();

    List<StagiaireResponseDTO> getSansStage();

    StagiaireResponseDTO searchByEmail(String email);

    AffectationEncadrantAcademiqueResponse affecterEncadrantAcademique(Long stagiaireId, Long encadrantId);

    void delete(Long id);
}
