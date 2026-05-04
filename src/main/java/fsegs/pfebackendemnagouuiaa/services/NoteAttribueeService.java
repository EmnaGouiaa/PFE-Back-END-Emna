package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;

import java.util.List;

public interface NoteAttribueeService {

    NoteAttribueeDto create(NoteAttribueeDto dto);

    NoteAttribueeDto getById(Long ficheEvaluationId, Long critereEvaluationId);

    List<NoteAttribueeDto> getAll();

    List<NoteAttribueeDto> getByFicheEvaluationId(Long ficheEvaluationId);

    List<NoteAttribueeDto> getByCritereEvaluationId(Long critereEvaluationId);

    NoteAttribueeDto update(Long ficheEvaluationId, Long critereEvaluationId, NoteAttribueeDto dto);

    void delete(Long ficheEvaluationId, Long critereEvaluationId);
}