package fsegs.pfebackendemnagouuiaa;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.User;
import fsegs.pfebackendemnagouuiaa.entities.Etudiant;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@SpringBootApplication
@EntityScan("fsegs.pfebackendemnagouuiaa.entities")
@EnableJpaRepositories("fsegs.pfebackendemnagouuiaa.repository")
public class PfeBackEndEmnaGouuiaaApplication {
	public static void main(String[] args) {
		SpringApplication.run(PfeBackEndEmnaGouuiaaApplication.class, args);
	}

	@Component
	public static class AdminUserInitializer implements CommandLineRunner {
		private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);
		private static final String DEFAULT_PASSWORD = "Password123!";

		private final UserRepository userRepository;
		private final BCryptPasswordEncoder passwordEncoder;

		public AdminUserInitializer(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
			this.userRepository = userRepository;
			this.passwordEncoder = passwordEncoder;
		}

		@Override
		public void run(String... args) throws Exception {
			// ADMIN - Base User class
			createOrUpdateUser("admin@pfe.tn", "Admin", "User", Role.ADMIN, "ADMIN001");
			
			// STAGIAIRE - Etudiant subclass (requires matricule)
			createOrUpdateEtudiant("student@pfe.tn", "John", "Doe", "MAT001", "Informatique", "3ème", "PFE");
			
			// ENCADRANT_ACADEMIQUE - EncadrantAcademique subclass
			createOrUpdateEncadrantAcademique("teacher@pfe.tn", "Sarah", "Smith", "Professeur", "Génie Logiciel", "Département Info");
			
			// RESPONSABLE_ENTREPRISE - ResponsableEntreprise subclass
			createOrUpdateResponsableEntreprise("company@pfe.tn", "Mike", "Johnson", "Rue 123", "IT", "+216 12 345 678", "Directeur");
			
			// ENCADRANT_PROFESSIONNEL - EncadrantProfessionnel subclass
			createOrUpdateEncadrantProfessionnel("supervisor@pfe.tn", "Lisa", "Williams", "Chef de Projet", "Développement");
			
			// RESPONSABLE_SERVICE_STAGES - Base User class
			createOrUpdateUser("internship@pfe.tn", "David", "Brown", Role.RESPONSABLE_SERVICE_STAGES, "RS001");
		}

		private void createOrUpdateUser(String email, String prenom, String nom, Role role, String matricule) {
			Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
			
			if (!existingUser.isPresent()) {
				User user = User.builder()
						.prenom(prenom)
						.nom(nom)
						.email(email)
						.password(passwordEncoder.encode(DEFAULT_PASSWORD))
						.compteValide(true)
						.role(role)
						.matricule(matricule)
						.build();

				userRepository.save(user);
				logger.info("{} user created successfully with email: {}", role, email);
			} else {
				User user = existingUser.get();
				// Verify and update if needed
				boolean needsUpdate = false;
				
				// Check if password needs reset (for testing purposes)
				if (!passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword())) {
					user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
					needsUpdate = true;
					logger.warn("Password for {} updated to default", email);
				}
				
				// Ensure account is validated
				if (!user.getCompteValide()) {
					user.setCompteValide(true);
					needsUpdate = true;
					logger.warn("Account validation enabled for {}", email);
				}
				
				// Ensure correct role
				if (user.getRole() != role) {
					user.setRole(role);
					needsUpdate = true;
					logger.warn("Role updated to {} for {}", role, email);
				}
				
				if (needsUpdate) {
					userRepository.save(user);
					logger.info("{} user updated successfully with email: {}", role, email);
				} else {
					logger.info("{} user already exists and is up-to-date with email: {}", role, email);
				}
			}
		}

		private void createOrUpdateEtudiant(String email, String prenom, String nom, String matricule, String filiere, String niveau, String niveauStage) {
			Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
			
			if (!existingUser.isPresent()) {
				Etudiant etudiant = new Etudiant();
				etudiant.setPrenom(prenom);
				etudiant.setNom(nom);
				etudiant.setEmail(email);
				etudiant.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
				etudiant.setCompteValide(true);
				etudiant.setRole(Role.STAGIAIRE);
				etudiant.setMatricule(matricule);
				etudiant.setFiliere(filiere);
				etudiant.setNiveau(niveau);
				etudiant.setNiveauStage(niveauStage);

				userRepository.save(etudiant);
				logger.info("STAGIAIRE user created successfully with email: {}", email);
			} else {
				logger.info("STAGIAIRE user already exists with email: {}", email);
			}
		}

		private void createOrUpdateEncadrantAcademique(String email, String prenom, String nom, String grade, String specialite, String departement) {
			Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
			
			if (!existingUser.isPresent()) {
				EncadrantAcademique encadrant = new EncadrantAcademique();
				encadrant.setPrenom(prenom);
				encadrant.setNom(nom);
				encadrant.setEmail(email);
				encadrant.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
				encadrant.setCompteValide(true);
				encadrant.setRole(Role.ENCADRANT_ACADEMIQUE);
				encadrant.setMatricule("ACAD-" + System.currentTimeMillis());
				encadrant.setGrade(grade);
				encadrant.setSpecialite(specialite);
				encadrant.setDepartement(departement);

				userRepository.save(encadrant);
				logger.info("ENCADRANT_ACADEMIQUE user created successfully with email: {}", email);
			} else {
				logger.info("ENCADRANT_ACADEMIQUE user already exists with email: {}", email);
			}
		}

		private void createOrUpdateEncadrantProfessionnel(String email, String prenom, String nom, String poste, String service) {
			Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
			
			if (!existingUser.isPresent()) {
				EncadrantProfessionnel encadrant = new EncadrantProfessionnel();
				encadrant.setPrenom(prenom);
				encadrant.setNom(nom);
				encadrant.setEmail(email);
				encadrant.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
				encadrant.setCompteValide(true);
				encadrant.setRole(Role.ENCADRANT_PROFESSIONNEL);
				encadrant.setMatricule("PROF-" + System.currentTimeMillis());
				encadrant.setPoste(poste);
				encadrant.setService(service);

				userRepository.save(encadrant);
				logger.info("ENCADRANT_PROFESSIONNEL user created successfully with email: {}", email);
			} else {
				logger.info("ENCADRANT_PROFESSIONNEL user already exists with email: {}", email);
			}
		}

		private void createOrUpdateResponsableEntreprise(String email, String prenom, String nom, String adresse, String secteurActivite, String telephone, String poste) {
			Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
			
			if (!existingUser.isPresent()) {
				ResponsableEntreprise responsable = new ResponsableEntreprise();
				responsable.setPrenom(prenom);
				responsable.setNom(nom);
				responsable.setEmail(email);
				responsable.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
				responsable.setCompteValide(true);
				responsable.setRole(Role.RESPONSABLE_ENTREPRISE);
				responsable.setMatricule("RESP-" + System.currentTimeMillis());
				responsable.setAdresse(adresse);
				responsable.setSecteurActivite(secteurActivite);
				responsable.setTelephone(telephone);
				responsable.setPoste(poste);

				userRepository.save(responsable);
				logger.info("RESPONSABLE_ENTREPRISE user created successfully with email: {}", email);
			} else {
				logger.info("RESPONSABLE_ENTREPRISE user already exists with email: {}", email);
			}
		}
	}
}