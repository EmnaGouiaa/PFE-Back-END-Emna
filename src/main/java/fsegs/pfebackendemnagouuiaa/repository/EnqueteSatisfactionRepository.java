package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.EnqueteSatisfaction;
import fsegs.pfebackendemnagouuiaa.entities.StatutEnqueteSatisfaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnqueteSatisfactionRepository extends JpaRepository<EnqueteSatisfaction, Long> {

    List<EnqueteSatisfaction> findByStageIdOrderByDateCreationAsc(Long stageId);

    List<EnqueteSatisfaction> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);

    List<EnqueteSatisfaction> findByUtilisateurIdAndStatutEnqueteOrderByDateCreationDesc(Long utilisateurId,
                                                                                         StatutEnqueteSatisfaction statutEnquete);

    Optional<EnqueteSatisfaction> findByStageIdAndUtilisateurId(Long stageId, Long utilisateurId);

    boolean existsByStageIdAndUtilisateurId(Long stageId, Long utilisateurId);
}
