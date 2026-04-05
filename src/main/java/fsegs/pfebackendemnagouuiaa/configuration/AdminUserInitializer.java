package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.User;
import fsegs.pfebackendemnagouuiaa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("🔧 Checking ADMIN user...");

        // Find any existing admin user
        Optional<User> existingAdmin = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst();

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            String password = admin.getPassword();
            
            log.info("Found ADMIN user: {} (ID: {})", admin.getEmail(), admin.getId());
            log.info("Password starts with $2a: {}", password != null && password.startsWith("$2a"));
            
            // Check if password is NOT BCrypt encoded
            if (password == null || !password.startsWith("$2")) {
                log.warn("⚠️ ADMIN password is NOT BCrypt encoded! Re-encoding...");
                
                // Re-encode the password
                String defaultPassword = "admin123"; // Change this or keep current plain password
                admin.setPassword(passwordEncoder.encode(defaultPassword));
                admin.setCompteValide(true);
                userRepository.save(admin);
                
                log.info("✅ ADMIN password re-encoded successfully!");
                log.info("📧 Email: {}", admin.getEmail());
                log.info("🔑 New password: {}", defaultPassword);
            } else {
                log.info("✅ ADMIN password is already BCrypt encoded");
                
                // Ensure compteValide is true
                if (Boolean.FALSE.equals(admin.getCompteValide())) {
                    admin.setCompteValide(true);
                    userRepository.save(admin);
                    log.info("✅ Set compteValide to true for ADMIN");
                }
            }
        } else {
            log.info("No ADMIN user found. Creating default ADMIN...");
            
            User admin = User.builder()
                    .prenom("Admin")
                    .nom("System")
                    .email("admin@pfe.tn")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .compteValide(true)
                    .matricule("ADMIN001")
                    .build();
            
            userRepository.save(admin);
            log.info("✅ Default ADMIN created: admin@pfe.tn / admin123");
        }
    }
}
