package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.mapper.EncadrantAcademiqueMapper;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.services.EncadrantAcademiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EncadrantAcademiqueServiceImpl implements EncadrantAcademiqueService {

    private final EncadrantAcademiqueRepository repository;
    private final EncadrantAcademiqueMapper mapper;

    @Override
    public EncadrantAcademiqueDto create(EncadrantAcademiqueDto dto) {
        EncadrantAcademique entity = mapper.toEntity(dto);
        EncadrantAcademique saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override
    public EncadrantAcademiqueDto update(Long id, EncadrantAcademiqueDto dto) {
        EncadrantAcademique entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encadrant introuvable"));

        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setGrade(dto.getGrade());
        entity.setMatricule(dto.getMatricule());
        entity.setSpecialite(dto.getSpecialite());

        return mapper.toDto(repository.save(entity));
    }

    @Override
    public EncadrantAcademiqueDto getById(Long id) {
        return mapper.toDto(
                repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Encadrant introuvable"))
        );
    }

    @Override
    public List<EncadrantAcademiqueDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
