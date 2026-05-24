package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.NoteAttribueeMapper;
import fsegs.pfebackendemnagouuiaa.repository.CritereEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.NoteAttribueeRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteAttribueeServiceImpl implements NoteAttribueeService {

    private final NoteAttribueeRepository noteAttribueeRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final CritereEvaluationRepository critereEvaluationRepository;
    private final NoteAttribueeMapper noteAttribueeMapper;
    private final JwtService jwtService;

    @Override
    @Transactional
    public NoteAttribueeDto create(NoteAttribueeDto dto) {
        validerDonnees(dto);

        FicheEvaluation fiche = ficheEvaluationRepository.findById(dto.getFicheEvaluationId())
                .orElseThrow(() -> new EntityNotFoundException("Fiche d'evaluation introuvable."));
        ensureProfessionalSupervisorEditor(fiche);

        CritereEvaluation critere = critereEvaluationRepository.findById(dto.getCritereEvaluationId())
                .orElseThrow(() -> new EntityNotFoundException("Critere d'evaluation introuvable."));

        CleNoteAttribuee id = new CleNoteAttribuee(dto.getFicheEvaluationId(), dto.getCritereEvaluationId());
        if (noteAttribueeRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Une note existe deja pour cette fiche et ce critere.");
        }
        if (fiche.estVerrouillee()) {
            throw new IllegalStateException("Impossible d'ajouter une note : la fiche d'evaluation est verrouillee.");
        }

        NoteAttribuee entity = noteAttribueeMapper.toEntity(dto);
        entity.setId(id);
        entity.setFicheEvaluation(fiche);
        entity.setCritereEvaluation(critere);
        entity.setBareme(dto.getBareme() == null ? 5 : dto.getBareme());

        NoteAttribuee saved = noteAttribueeRepository.save(entity);
        recalculerNoteFinale(fiche);
        return noteAttribueeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NoteAttribueeDto getById(Long ficheEvaluationId, Long critereEvaluationId) {
        CleNoteAttribuee id = new CleNoteAttribuee(ficheEvaluationId, critereEvaluationId);
        NoteAttribuee entity = noteAttribueeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note attribuee introuvable."));
        ensureCanView(entity.getFicheEvaluation());
        return noteAttribueeMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteAttribueeDto> getAll() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        return noteAttribueeRepository.findAll().stream()
                .filter(item -> canView(item.getFicheEvaluation(), utilisateur))
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteAttribueeDto> getByFicheEvaluationId(Long ficheEvaluationId) {
        FicheEvaluation fiche = ficheEvaluationRepository.findById(ficheEvaluationId)
                .orElseThrow(() -> new EntityNotFoundException("Fiche d'evaluation introuvable."));
        ensureCanView(fiche);
        return noteAttribueeRepository.findByFicheEvaluationId(ficheEvaluationId).stream()
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteAttribueeDto> getByCritereEvaluationId(Long critereEvaluationId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        return noteAttribueeRepository.findByCritereEvaluationId(critereEvaluationId).stream()
                .filter(item -> canView(item.getFicheEvaluation(), utilisateur))
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public NoteAttribueeDto update(Long ficheEvaluationId, Long critereEvaluationId, NoteAttribueeDto dto) {
        CleNoteAttribuee id = new CleNoteAttribuee(ficheEvaluationId, critereEvaluationId);
        NoteAttribuee entity = noteAttribueeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note attribuee introuvable."));

        ensureProfessionalSupervisorEditor(entity.getFicheEvaluation());
        if (entity.getFicheEvaluation() != null && entity.getFicheEvaluation().estVerrouillee()) {
            throw new IllegalStateException("Impossible de modifier une note : la fiche d'evaluation est verrouillee.");
        }
        if (dto.getFicheEvaluationId() != null && !dto.getFicheEvaluationId().equals(ficheEvaluationId)) {
            throw new IllegalArgumentException("La fiche d'evaluation d'une note ne peut pas etre modifiee.");
        }
        if (dto.getCritereEvaluationId() != null && !dto.getCritereEvaluationId().equals(critereEvaluationId)) {
            throw new IllegalArgumentException("Le critere d'evaluation d'une note ne peut pas etre modifie.");
        }

        validerDonnees(dto);
        entity.setPoids(dto.getPoids());
        entity.setBareme(dto.getBareme() == null ? 5 : dto.getBareme());
        entity.setNote(dto.getNote());
        entity.setCommentaire(dto.getCommentaire());

        NoteAttribuee updated = noteAttribueeRepository.save(entity);
        recalculerNoteFinale(entity.getFicheEvaluation());
        return noteAttribueeMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long ficheEvaluationId, Long critereEvaluationId) {
        CleNoteAttribuee id = new CleNoteAttribuee(ficheEvaluationId, critereEvaluationId);
        NoteAttribuee entity = noteAttribueeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note attribuee introuvable."));

        ensureProfessionalSupervisorEditor(entity.getFicheEvaluation());
        if (entity.getFicheEvaluation() != null && entity.getFicheEvaluation().estVerrouillee()) {
            throw new IllegalStateException("Impossible de supprimer une note : la fiche d'evaluation est verrouillee.");
        }

        noteAttribueeRepository.delete(entity);
        recalculerNoteFinale(entity.getFicheEvaluation());
    }

    private void validerDonnees(NoteAttribueeDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Les donnees de note sont obligatoires.");
        }
        if (dto.getFicheEvaluationId() == null) {
            throw new IllegalArgumentException("La fiche d'evaluation est obligatoire.");
        }
        if (dto.getCritereEvaluationId() == null) {
            throw new IllegalArgumentException("Le critere d'evaluation est obligatoire.");
        }
        if (dto.getPoids() == null || dto.getPoids() <= 0) {
            throw new IllegalArgumentException("Le coefficient doit etre superieur a zero.");
        }
        if (dto.getBareme() == null) {
            dto.setBareme(5);
        }
        if (dto.getBareme() <= 0) {
            throw new IllegalArgumentException("Le bareme doit etre superieur a zero.");
        }
        if (dto.getNote() == null) {
            throw new IllegalArgumentException("La note est obligatoire.");
        }
        if (dto.getNote() < 0) {
            throw new IllegalArgumentException("La note ne peut pas etre negative.");
        }
        if (dto.getNote() > dto.getBareme()) {
            throw new IllegalArgumentException("La note ne peut pas depasser le bareme.");
        }
    }

    private void recalculerNoteFinale(FicheEvaluation fiche) {
        if (fiche == null) {
            return;
        }
        fiche.setNoteFinale(fiche.calculerNoteFinale());
        ficheEvaluationRepository.save(fiche);
    }

    private void ensureProfessionalSupervisorEditor(FicheEvaluation fiche) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        Stage stage = fiche == null ? null : fiche.getStage();
        boolean authorized = utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage != null
                && stage.getEncadrantProfessionnel() != null
                && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
        if (!authorized) {
            throw new AccessDeniedException("Seul l'encadrant professionnel affecte au stage peut modifier les notes.");
        }
    }

    private void ensureCanView(FicheEvaluation fiche) {
        if (!canView(fiche, getAuthenticatedUtilisateur())) {
            throw new AccessDeniedException("Acces non autorise a cette fiche d'evaluation.");
        }
    }

    private boolean canView(FicheEvaluation fiche, Utilisateur utilisateur) {
        if (fiche == null || utilisateur == null || fiche.getStage() == null) {
            return false;
        }
        Stage stage = fiche.getStage();
        return switch (utilisateur.getRole()) {
            case ADMINISTRATEUR,
                    RESPONSABLE_STAGE -> true;
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null
                    && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
            case RESPONSABLE_ENTREPRISE -> {
                if (!(utilisateur instanceof ResponsableEntreprise re)) yield false;
                yield re.getEntreprise() != null
                        && stage.getEntreprise() != null
                        && re.getEntreprise().getId().equals(stage.getEntreprise().getId());
            }
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null
                    && stage.getEncadrantAcademique().getId().equals(utilisateur.getId());
            case STAGIAIRE -> stage.getStagiaire() != null
                    && stage.getStagiaire().getId().equals(utilisateur.getId());
            default -> false;
        };
    }

    private Utilisateur getAuthenticatedUtilisateur() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
    }
}
