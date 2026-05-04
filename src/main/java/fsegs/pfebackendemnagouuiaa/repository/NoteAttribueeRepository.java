package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteAttribueeRepository extends JpaRepository<NoteAttribuee, CleNoteAttribuee> {

    List<NoteAttribuee> findByFicheEvaluationId(Long ficheEvaluationId);

    List<NoteAttribuee> findByCritereEvaluationId(Long critereEvaluationId);
}