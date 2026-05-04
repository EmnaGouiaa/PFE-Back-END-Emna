package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesResponseDTO;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.mapper.ResponsableServiceStagesMapper;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableServiceStagesRepository;
import fsegs.pfebackendemnagouuiaa.services.ResponsableServiceStagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponsableServiceStagesServiceImpl implements ResponsableServiceStagesService {

    private final ResponsableServiceStagesRepository repository;
    private final ResponsableServiceStagesMapper mapper;

    @Override
    public ResponsableServiceStagesResponseDTO create(ResponsableServiceStagesRequestDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        ResponsableServiceStages responsable = mapper.toEntity(dto);
        ResponsableServiceStages saved = repository.save(responsable);

        return mapper.toResponseDTO(saved);
    }

    @Override
    public ResponsableServiceStagesResponseDTO update(Long id, ResponsableServiceStagesRequestDTO dto) {
        ResponsableServiceStages existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ResponsableServiceStages introuvable avec id : " + id));

        if (dto.getEmail() != null &&
                !dto.getEmail().equals(existing.getEmail()) &&
                repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        mapper.updateEntityFromDto(dto, existing);
        ResponsableServiceStages updated = repository.save(existing);

        return mapper.toResponseDTO(updated);
    }

    @Override
    public ResponsableServiceStagesResponseDTO getById(Long id) {
        ResponsableServiceStages responsable = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ResponsableServiceStages introuvable avec id : " + id));

        return mapper.toResponseDTO(responsable);
    }

    @Override
    public List<ResponsableServiceStagesResponseDTO> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public void delete(Long id) {
        ResponsableServiceStages responsable = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ResponsableServiceStages introuvable avec id : " + id));

        repository.delete(responsable);
    }
}