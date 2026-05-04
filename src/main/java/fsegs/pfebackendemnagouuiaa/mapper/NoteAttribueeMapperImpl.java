package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.mapper.NoteAttribueeMapper;
import org.springframework.stereotype.Component;

@Component
public class NoteAttribueeMapperImpl implements NoteAttribueeMapper {

    @Override
    public NoteAttribueeDto toDto(NoteAttribuee entity) {
        if (entity == null) {
            return null;
        }

        NoteAttribueeDto dto = new NoteAttribueeDto();

        if (entity.getId() != null) {
            dto.setFicheEvaluationId(entity.getId().getFicheId());
            dto.setCritereEvaluationId(entity.getId().getCritereId());
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

    @Override
    public NoteAttribuee toEntity(NoteAttribueeDto dto) {
        if (dto == null) {
            return null;
        }

        NoteAttribuee entity = new NoteAttribuee();

        if (dto.getFicheEvaluationId() != null && dto.getCritereEvaluationId() != null) {
            entity.setId(new CleNoteAttribuee(dto.getCritereEvaluationId(), dto.getFicheEvaluationId()));
        }

        entity.setPoids(dto.getPoids());
        entity.setBareme(dto.getBareme());
        entity.setNote(dto.getNote());
        entity.setCommentaire(dto.getCommentaire());

        return entity;
    }
}