package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.mapper.NoteAttribueeMapper;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link NoteAttribueeMapper}.
 * <p>
 * Gère la clé composite {@link CleNoteAttribuee} et les champs calculés côté entité.
 * Utilisée par {@link fsegs.pfebackendemnagouuiaa.services.NoteAttribueeServiceImpl} et
 * {@link FicheEvaluationMapperImpl}.
 */
@Component
public class NoteAttribueeMapperImpl implements NoteAttribueeMapper {

    /**
     * {@inheritDoc}
     * <p>
     * Déploie l'identifiant embarqué en {@code ficheEvaluationId} / {@code critereEvaluationId}.
     * {@code evaluee} et {@code scorePondere} sont calculés par l'entité, pas recopiés du DTO entrant.
     */
    @Override
    public NoteAttribueeDto toDto(NoteAttribuee entity) {
        if (entity == null) {
            return null;
        }

        NoteAttribueeDto dto = new NoteAttribueeDto();

        if (entity.getId() != null) {
            dto.setFicheEvaluationId(entity.getId().getFicheEvaluationId());
            dto.setCritereEvaluationId(entity.getId().getCritereEvaluationId());
        }

        dto.setPoids(entity.getPoids());
        dto.setBareme(entity.getBareme());
        dto.setNote(entity.getNote());
        dto.setCommentaire(entity.getCommentaire());
        dto.setEvaluee(entity.estEvalue());
        dto.setScorePondere(entity.calculerScorePondere());

        if (entity.getCritereEvaluation() != null) {
            dto.setCritereLibelle(entity.getCritereEvaluation().getLibelle());
        }

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * La clé composite n'est créée que si les deux identifiants sont non nuls.
     */
    @Override
    public NoteAttribuee toEntity(NoteAttribueeDto dto) {
        if (dto == null) {
            return null;
        }

        NoteAttribuee entity = new NoteAttribuee();

        if (dto.getFicheEvaluationId() != null && dto.getCritereEvaluationId() != null) {
            entity.setId(new CleNoteAttribuee(dto.getFicheEvaluationId(), dto.getCritereEvaluationId()));
        }

        entity.setPoids(dto.getPoids());
        entity.setBareme(dto.getBareme());
        entity.setNote(dto.getNote());
        entity.setCommentaire(dto.getCommentaire());

        return entity;
    }
}
