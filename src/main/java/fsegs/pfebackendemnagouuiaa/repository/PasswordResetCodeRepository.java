package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    List<PasswordResetCode> findByEmailIgnoreCaseAndUsedFalse(String email);

    Optional<PasswordResetCode> findFirstByEmailIgnoreCaseAndCodeAndUsedFalseOrderByExpirationDateDesc(String email, String code);

    void deleteByExpirationDateBefore(LocalDateTime dateTime);
}
