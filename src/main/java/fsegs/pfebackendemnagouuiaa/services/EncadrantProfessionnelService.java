package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;

import java.util.List;

public interface EncadrantProfessionnelService {
    EncadrantProfessionnelDto create(EncadrantProfessionnelDto dto);

    EncadrantProfessionnelDto update(Long id, EncadrantProfessionnelDto dto);

    EncadrantProfessionnelDto getById(Long id);

    List<EncadrantProfessionnelDto> getAll();

    List<EncadrantProfessionnelDto> getByEntrepriseId(Long entrepriseId);

    EncadrantProfessionnelDto createByResponsableEntreprise(Long responsableId, EncadrantProfessionnelDto dto);

    void delete(Long id);
}
