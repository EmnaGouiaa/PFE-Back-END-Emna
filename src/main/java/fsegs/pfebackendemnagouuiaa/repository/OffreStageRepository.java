package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.OffreStage;
import fsegs.pfebackendemnagouuiaa.entities.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffreStageRepository extends JpaRepository<OffreStage, Long> {

    List<OffreStage> findByEntrepriseId(Long entrepriseId);

    List<OffreStage> findByPublieeParId(Long responsableEntrepriseId);

    List<OffreStage> findByValideeParId(Long responsableServiceStagesId);

    List<OffreStage> findByStatut(StatutOffre statut);

    List<OffreStage> findByStatutOrderByIdDesc(StatutOffre statut);

    List<OffreStage> findByStatutInOrderByDatePublicationDescIdDesc(List<StatutOffre> statuts);

    List<OffreStage> findAllByOrderByIdDesc();

    List<OffreStage> findByTitreContainingIgnoreCase(String titre);
}
