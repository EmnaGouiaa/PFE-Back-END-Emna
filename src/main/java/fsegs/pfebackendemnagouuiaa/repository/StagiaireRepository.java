package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StagiaireRepository extends JpaRepository<Stagiaire, Long> {

    Optional<Stagiaire> findByEmail(String email);

    Optional<Stagiaire> findByEmailIgnoreCase(String email);

    @Query("select s from Stagiaire s where lower(trim(s.email)) = lower(trim(:email))")
    Optional<Stagiaire> findByNormalizedEmail(@Param("email") String email);

    Optional<Stagiaire> findByMatricule(String matricule);

    boolean existsByEmail(String email);

    boolean existsByMatricule(String matricule);
}
