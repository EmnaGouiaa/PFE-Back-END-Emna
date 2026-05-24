package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SignatureRepository extends JpaRepository<Signature, Long> {

    @Query("SELECT s FROM ConventionStage c JOIN c.signatures s WHERE c.id = :id AND s.roleSignature = :role")
    Optional<Signature> findByConventionStageIdAndRoleSignature(@Param("id") Long id, @Param("role") RoleSignature role);

    @Query("SELECT s FROM CahierStage c JOIN c.signatures s WHERE c.id = :id AND s.roleSignature = :role")
    Optional<Signature> findByCahierStageIdAndRoleSignature(@Param("id") Long id, @Param("role") RoleSignature role);

    @Query("SELECT s FROM FicheEvaluation f JOIN f.signatures s WHERE f.id = :id AND s.roleSignature = :role")
    Optional<Signature> findByFicheEvaluationIdAndRoleSignature(@Param("id") Long id, @Param("role") RoleSignature role);
}
