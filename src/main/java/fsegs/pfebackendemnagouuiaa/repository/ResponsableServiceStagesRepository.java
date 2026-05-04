package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponsableServiceStagesRepository extends JpaRepository<ResponsableServiceStages, Long> {

    Optional<ResponsableServiceStages> findByEmail(String email);

    boolean existsByEmail(String email);
}