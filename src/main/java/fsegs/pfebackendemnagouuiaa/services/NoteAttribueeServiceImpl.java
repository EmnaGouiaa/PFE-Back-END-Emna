package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.mapper.NoteAttribueeMapper;
import fsegs.pfebackendemnagouuiaa.repository.CritereEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.NoteAttribueeRepository;
import fsegs.pfebackendemnagouuiaa.services.NoteAttribueeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteAttribueeServiceImpl implements NoteAttribueeService {

    private final NoteAttribueeRepository noteAttribueeRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final CritereEvaluationRepository critereEvaluationRepository;
    private final NoteAttribueeMapper noteAttribueeMapper;

    @Override
    public NoteAttribueeDto create(NoteAttribueeDto dto) {
        validerDonnees(dto);

        FicheEvaluation fiche = ficheEvaluationRepository.findById(dto.getFicheEvaluationId())
                .orElseThrow(() -> new RuntimeException("FicheEvaluation introuvable avec l'id : " + dto.getFicheEvaluationId()));

        CritereEvaluation critere = critereEvaluationRepository.findById(dto.getCritereEvaluationId())
                .orElseThrow(() -> new RuntimeException("CritereEvaluation introuvable avec l'id : " + dto.getCritereEvaluationId()));

        CleNoteAttribuee id = new CleNoteAttribuee(dto.getCritereEvaluationId(), dto.getFicheEvaluationId());

        if (noteAttribueeRepository.findById(id).isPresent()) {
            throw new RuntimeException("Une note existe déjà pour cette fiche et ce critère");
        }
        if (fiche.estVerrouillee()) {
            throw new RuntimeException("Impossible d'ajouter une note : la fiche d'évaluation est verrouillée");
        }
        NoteAttribuee entity = noteAttribueeMapper.toEntity(dto);
        entity.setId(id);
        entity.setFicheEvaluation(fiche);
        entity.setCritereEvaluation(critere);

        NoteAttribuee saved = noteAttribueeRepository.save(entity);
        return noteAttribueeMapper.toDto(saved);
    }

    @Override
    public NoteAttribueeDto getById(Long ficheEvaluationId, Long critereEvaluationId) {
        CleNoteAttribuee id = new CleNoteAttribuee(critereEvaluationId, ficheEvaluationId);

        NoteAttribuee entity = noteAttribueeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NoteAttribuee introuvable"));

        return noteAttribueeMapper.toDto(entity);
    }

    @Override
    public List<NoteAttribueeDto> getAll() {
        return noteAttribueeRepository.findAll()
                .stream()
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    @Override
    public List<NoteAttribueeDto> getByFicheEvaluationId(Long ficheEvaluationId) {
        return noteAttribueeRepository.findByFicheEvaluationId(ficheEvaluationId)
                .stream()
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    @Override
    public List<NoteAttribueeDto> getByCritereEvaluationId(Long critereEvaluationId) {
        return noteAttribueeRepository.findByCritereEvaluationId(critereEvaluationId)
                .stream()
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    @Override
    public NoteAttribueeDto update(Long ficheEvaluationId, Long critereEvaluationId, NoteAttribueeDto dto) {
        CleNoteAttribuee id = new CleNoteAttribuee(critereEvaluationId, ficheEvaluationId);

        NoteAttribuee entity = noteAttribueeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NoteAttribuee introuvable"));

        if (entity.getFicheEvaluation() != null && entity.getFicheEvaluation().estVerrouillee()) {
            throw new RuntimeException("Impossible de modifier une note : la fiche d'évaluation est verrouillée");
        }

        if (dto.getFicheEvaluationId() != null && !dto.getFicheEvaluationId().equals(ficheEvaluationId)) {
            throw new RuntimeException("La fiche d'évaluation d'une note ne peut pas être modifiée");
        }

        if (dto.getCritereEvaluationId() != null && !dto.getCritereEvaluationId().equals(critereEvaluationId)) {
            throw new RuntimeException("Le critère d'évaluation d'une note ne peut pas être modifié");
        }

        if (dto.getPoids() == null) {
            throw new RuntimeException("Le poids est obligatoire");
        }

        if (dto.getBareme() == null) {
            throw new RuntimeException("Le barème est obligatoire");
        }

        if (dto.getNote() == null) {
            throw new RuntimeException("La note est obligatoire");
        }

        if (dto.getPoids() <= 0) {
            throw new RuntimeException("Le poids doit être supérieur à 0");
        }

        if (dto.getBareme() <= 0) {
            throw new RuntimeException("Le barème doit être supérieur à 0");
        }

        if (dto.getNote() < 0) {
            throw new RuntimeException("La note ne peut pas être négative");
        }

        if (dto.getNote() > dto.getBareme()) {
            throw new RuntimeException("La note ne peut pas dépasser le barème");
        }

        entity.setPoids(dto.getPoids());
        entity.setBareme(dto.getBareme());
        entity.setNote(dto.getNote());
        entity.setCommentaire(dto.getCommentaire());

        NoteAttribuee updated = noteAttribueeRepository.save(entity);
        return noteAttribueeMapper.toDto(updated);
    }
    @Override
    public void delete(Long ficheEvaluationId, Long critereEvaluationId) {
        CleNoteAttribuee id = new CleNoteAttribuee(critereEvaluationId, ficheEvaluationId);

        NoteAttribuee entity = noteAttribueeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NoteAttribuee introuvable"));

        noteAttribueeRepository.delete(entity);
    }
    private void validerDonnees(NoteAttribueeDto dto) {
        if (dto.getFicheEvaluationId() == null) {
            throw new RuntimeException("La fiche d'évaluation est obligatoire");
        }

        if (dto.getCritereEvaluationId() == null) {
            throw new RuntimeException("Le critère d'évaluation est obligatoire");
        }

        if (dto.getPoids() == null) {
            throw new RuntimeException("Le poids est obligatoire");
        }

        if (dto.getBareme() == null) {
            throw new RuntimeException("Le barème est obligatoire");
        }

        if (dto.getNote() == null) {
            throw new RuntimeException("La note est obligatoire");
        }

        if (dto.getPoids() <= 0) {
            throw new RuntimeException("Le poids doit être supérieur à 0");
        }

        if (dto.getBareme() <= 0) {
            throw new RuntimeException("Le barème doit être supérieur à 0");
        }

        if (dto.getNote() < 0) {
            throw new RuntimeException("La note ne peut pas être négative");
        }

        if (dto.getNote() > dto.getBareme()) {
            throw new RuntimeException("La note ne peut pas dépasser le barème");
        }
    }
    private void validerValeursNote(Integer poids, Integer bareme, Integer note) {
        if (poids == null) {
            throw new RuntimeException("Le poids est obligatoire");
        }
        if (bareme == null) {
            throw new RuntimeException("Le barème est obligatoire");
        }
        if (note == null) {
            throw new RuntimeException("La note est obligatoire");
        }
        if (poids <= 0) {
            throw new RuntimeException("Le poids doit être supérieur à 0");
        }
        if (bareme <= 0) {
            throw new RuntimeException("Le barème doit être supérieur à 0");
        }
        if (note < 0) {
            throw new RuntimeException("La note ne peut pas être négative");
        }
        if (note > bareme) {
            throw new RuntimeException("La note ne peut pas dépasser le barème");
        }
    }
    private void recalculerNoteFinale(FicheEvaluation fiche) {
        fiche.setNoteFinale(fiche.calculerNoteFinale());
        ficheEvaluationRepository.save(fiche);
    }
}