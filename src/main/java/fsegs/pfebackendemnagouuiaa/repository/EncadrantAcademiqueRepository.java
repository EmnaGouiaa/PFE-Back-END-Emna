package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncadrantAcademiqueRepository extends JpaRepository<EncadrantAcademique, Long> {

    Optional<EncadrantAcademique> findByMatricule(String matricule);

    boolean existsByMatricule(String matricule);
}
