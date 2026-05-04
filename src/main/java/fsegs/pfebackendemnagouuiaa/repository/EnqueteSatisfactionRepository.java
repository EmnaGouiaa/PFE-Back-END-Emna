package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.EnqueteSatisfaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnqueteSatisfactionRepository extends JpaRepository<EnqueteSatisfaction, Long> {

    Optional<EnqueteSatisfaction> findTopByOrderByIdAsc();
}
