package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponsableEntrepriseRepository extends JpaRepository<ResponsableEntreprise, Long> {

    Optional<ResponsableEntreprise> findByEmail(String email);

    Optional<ResponsableEntreprise> findByEmailIgnoreCase(String email);

    List<ResponsableEntreprise> findByEntrepriseId(Long entrepriseId);

    boolean existsByEmail(String email);
}
