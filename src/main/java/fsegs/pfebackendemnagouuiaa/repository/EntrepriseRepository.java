package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    
    Optional<Entreprise> findByNom(String nom);
    
    @Query("SELECT e FROM Entreprise e WHERE LOWER(e.nom) = LOWER(:nom)")
    Optional<Entreprise> findByNomIgnoreCase(@Param("nom") String nom);
    
    @Query("SELECT e FROM Entreprise e WHERE e.secteurActivite = :secteurActivite")
    List<Entreprise> findBySecteurActivite(@Param("secteurActivite") String secteurActivite);
    
    @Query("SELECT e FROM Entreprise e WHERE LOWER(e.email) = LOWER(:email)")
    Optional<Entreprise> findByEmailIgnoreCase(@Param("email") String email);
    
    @Query("SELECT e FROM Entreprise e WHERE e.telephone = :telephone")
    Optional<Entreprise> findByTelephone(@Param("telephone") String telephone);
    
    @Query("SELECT COUNT(e) FROM Entreprise e WHERE e.secteurActivite = :secteurActivite")
    Long countBySecteurActivite(@Param("secteurActivite") String secteurActivite);
    
    @Query("SELECT e FROM Entreprise e WHERE LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.secteurActivite) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Entreprise> searchByKeyword(@Param("keyword") String keyword);
}
