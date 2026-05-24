package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantAcademiqueDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.mapper.EncadrantAcademiqueMapper;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.services.EncadrantAcademiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EncadrantAcademiqueServiceImpl implements EncadrantAcademiqueService {

    private final EncadrantAcademiqueRepository repository;
    private final EncadrantAcademiqueMapper mapper;
    private final StageRepository stageRepository;

    @Override
    public EncadrantAcademiqueDto create(EncadrantAcademiqueDto dto) {
        EncadrantAcademique entity = mapper.toEntity(dto);
        EncadrantAcademique saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public EncadrantAcademiqueDto update(Long id, EncadrantAcademiqueDto dto) {
        EncadrantAcademique entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encadrant academique introuvable avec l'id : " + id));

        // Règle métier : interdire la modification si l'encadrant a déjà validé
        // le sujet d'un stage (statut sujet = VALIDEE, sujetValidePar = cet encadrant).
        if (stageRepository.existsBySujetValideParId(id)) {
            throw new IllegalStateException(
                    "Modification impossible : cet encadrant a deja valide le sujet d'au moins un stage. "
                    + "Aucune modification n'est permise une fois la validation effectuee.");
        }

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
