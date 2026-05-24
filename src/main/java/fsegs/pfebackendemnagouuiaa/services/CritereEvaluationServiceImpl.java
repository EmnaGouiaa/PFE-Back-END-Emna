package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.mapper.CritereEvaluationMapper;
import fsegs.pfebackendemnagouuiaa.repository.CritereEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.services.CritereEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CritereEvaluationServiceImpl implements CritereEvaluationService {

    private final CritereEvaluationRepository repository;
    private final FicheEvaluationRepository ficheRepository;
    private final CritereEvaluationMapper mapper;

    @Override
    public CritereEvaluationDto create(CritereEvaluationDto dto) {
        CritereEvaluation entity = mapper.toEntity(dto);

        if (dto.getFicheId() != null) {
            FicheEvaluation fiche = ficheRepository.findById(dto.getFicheId())
                    .orElseThrow(() -> new RuntimeException("Fiche introuvable"));
            entity.setFiche(fiche);
            if (fiche.estVerrouillee()) {
                throw new RuntimeException("Impossible d'ajouter un critére : la fiche d'évaluation est verrouillée");
            }
        }


        return mapper.toDto(repository.save(entity));
    }

    @Override
    public CritereEvaluationDto getById(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Critére introuvable")));
    }

    @Override
    public List<CritereEvaluationDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<CritereEvaluationDto> getByFicheId(Long ficheId) {
        return repository.findByFicheId(ficheId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public CritereEvaluationDto update(Long id, CritereEvaluationDto dto) {
        CritereEvaluation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Critére introuvable"));

        if (entity.getFiche() != null && entity.getFiche().estVerrouillee()) {
            throw new RuntimeException("Impossible de modifier un critére : la fiche d'évaluation est verrouillée");
        }

        if (dto.getPartie() == null) {
            throw new RuntimeException("La partie du critére est obligatoire");
        }

        entity.setLibelle(dto.getLibelle());
        entity.setDescription(dto.getDescription());
        entity.setCategorie(dto.getCategorie());
        entity.setBareme(dto.getBareme());
        entity.setConsigne(dto.getCommentaireGeneral());
        entity.setPartie(dto.getPartie());

        return mapper.toDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<CritereEvaluationDto> getGlobaux() {
        return repository.findByFicheIsNull()
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}