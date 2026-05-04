package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;

import java.util.List;

public interface EntrepriseService {

    EntrepriseDto create(EntrepriseDto dto);

    EntrepriseDto update(Long id, EntrepriseDto dto);

    EntrepriseDto getById(Long id);

    List<EntrepriseDto> getAll();

    void delete(Long id);
}
