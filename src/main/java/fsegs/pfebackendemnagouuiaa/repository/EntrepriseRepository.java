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

    Optional<Entreprise> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNom(String nom);
    boolean existsByTelephone(String telephone);

    Optional<Entreprise> findByEmailIgnoreCase(String email);
    Optional<Entreprise> findByTelephone(String telephone);

    Optional<Entreprise> findByNomIgnoreCase(String nom);

}
