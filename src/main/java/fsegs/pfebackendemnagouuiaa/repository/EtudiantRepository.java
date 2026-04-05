package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    
    Optional<Etudiant> findByMatricule(String matricule);
    
    @Query("SELECT e FROM Etudiant e WHERE LOWER(e.matricule) = LOWER(:matricule)")
    Optional<Etudiant> findByMatriculeIgnoreCase(@Param("matricule") String matricule);
    
    @Query("SELECT e FROM Etudiant e WHERE e.filiere = :filiere")
    List<Etudiant> findByFiliere(@Param("filiere") String filiere);
    
    @Query("SELECT e FROM Etudiant e WHERE e.niveau = :niveau")
    List<Etudiant> findByNiveau(@Param("niveau") String niveau);
    
    @Query("SELECT e FROM Etudiant e WHERE e.filiere = :filiere AND e.niveau = :niveau")
    List<Etudiant> findByFiliereAndNiveau(@Param("filiere") String filiere, @Param("niveau") String niveau);
    
    @Query("SELECT COUNT(e) FROM Etudiant e WHERE e.filiere = :filiere")
    Long countByFiliere(@Param("filiere") String filiere);
    
    @Query("SELECT COUNT(e) FROM Etudiant e WHERE e.niveau = :niveau")
    Long countByNiveau(@Param("niveau") String niveau);
}
