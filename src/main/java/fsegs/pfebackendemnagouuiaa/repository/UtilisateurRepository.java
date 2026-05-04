package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    boolean existsByTelephone(String telephone);
    boolean existsByMatricule(String matricule);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    @Query("select u from Utilisateur u where lower(trim(u.email)) = lower(trim(:email))")
    Optional<Utilisateur> findByNormalizedEmail(@Param("email") String email);
    List<Utilisateur> findByRole(Role role);
    Boolean existsByEmailIgnoreCase(String email);
    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<Utilisateur> findByTelephone(String telephone);
    Optional<Utilisateur> findByMatricule(String matricule);
}
