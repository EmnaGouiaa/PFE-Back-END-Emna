package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesResponseDTO;

import java.util.List;

public interface ResponsableServiceStagesService {

    ResponsableServiceStagesResponseDTO create(ResponsableServiceStagesRequestDTO dto);

    ResponsableServiceStagesResponseDTO update(Long id, ResponsableServiceStagesRequestDTO dto);

    ResponsableServiceStagesResponseDTO getById(Long id);

    List<ResponsableServiceStagesResponseDTO> getAll();

    void delete(Long id);
}