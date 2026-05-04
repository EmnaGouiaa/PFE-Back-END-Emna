package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;

import java.util.List;

public interface ResponsableEntrepriseService {

    ResponsableEntrepriseDto create(ResponsableEntrepriseDto dto);

    ResponsableEntrepriseDto update(Long id, ResponsableEntrepriseDto dto);

    ResponsableEntrepriseDto getById(Long id);

    List<ResponsableEntrepriseDto> getAll();

    List<ResponsableEntrepriseDto> getByEntrepriseId(Long entrepriseId);

    void delete(Long id);
}