package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncadrantProfessionnelRepository extends JpaRepository<EncadrantProfessionnel, Long> {
    
    @Query("SELECT e FROM EncadrantProfessionnel e WHERE e.poste = :poste")
    List<EncadrantProfessionnel> findByPoste(@Param("poste") String poste);
    
    @Query("SELECT e FROM EncadrantProfessionnel e WHERE e.service = :service")
    List<EncadrantProfessionnel> findByService(@Param("service") String service);
    
    @Query("SELECT e FROM EncadrantProfessionnel e WHERE e.entreprise.id = :entrepriseId")
    List<EncadrantProfessionnel> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT e FROM EncadrantProfessionnel e WHERE e.poste = :poste AND e.service = :service")
    List<EncadrantProfessionnel> findByPosteAndService(@Param("poste") String poste, @Param("service") String service);
    
    @Query("SELECT COUNT(e) FROM EncadrantProfessionnel e WHERE e.entreprise.id = :entrepriseId")
    Long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT COUNT(e) FROM EncadrantProfessionnel e WHERE e.poste = :poste")
    Long countByPoste(@Param("poste") String poste);
    
    @Query("SELECT e FROM EncadrantProfessionnel e WHERE " +
           "LOWER(e.poste) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.service) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<EncadrantProfessionnel> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT e FROM EncadrantProfessionnel e WHERE e.entreprise.id = :entrepriseId AND e.poste = :poste")
    List<EncadrantProfessionnel> findByEntrepriseIdAndPoste(@Param("entrepriseId") Long entrepriseId, @Param("poste") String poste);
}
