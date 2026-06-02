package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReunionRepository extends JpaRepository<Reunion, Long> {

    List<Reunion> findAllByOrderByDateDescHeureDesc();

    List<Reunion> findByStageId(Long stageId);

    List<Reunion> findByStageIdOrderByDateDescHeureDesc(Long stageId);

    List<Reunion> findByStageIdAndParticipants_IdOrderByDateDescHeureDesc(Long stageId, Long participantId);

    List<Reunion> findByParticipants_IdOrderByDateDescHeureDesc(Long participantId);

    List<Reunion> findByEncadrantCreateurIdOrderByDateDescHeureDesc(Long encadrantCreateurId);

    List<Reunion> findByStageStagiaireIdOrderByDateDescHeureDesc(Long stagiaireId);

    List<Reunion> findByStageEntrepriseIdOrderByDateDescHeureDesc(Long entrepriseId);

    boolean existsByStageIdAndDateAndHeure(Long stageId, LocalDate date, LocalTime heure);

    boolean existsByStageIdAndDateAndHeureAndIdNot(Long stageId, LocalDate date, LocalTime heure, Long id);

    Optional<Reunion> findByNumReunion(String numReunion);
}
