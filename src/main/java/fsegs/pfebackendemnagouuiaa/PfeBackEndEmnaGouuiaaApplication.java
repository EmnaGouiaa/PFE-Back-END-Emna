package fsegs.pfebackendemnagouuiaa;

import fsegs.pfebackendemnagouuiaa.entities.*;
import fsegs.pfebackendemnagouuiaa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class PfeBackEndEmnaGouuiaaApplication {

	public static void main(String[] args) {
		SpringApplication.run(PfeBackEndEmnaGouuiaaApplication.class, args);
	}

	@Bean
	CommandLineRunner init(UtilisateurRepository utilisateurRepository,
	                       PasswordEncoder passwordEncoder,
	                       EntrepriseRepository entrepriseRepository,
	                       FiliereRepository filiereRepository,
	                       StageRepository stageRepository) {
		return args -> {

			// =========================
			// ENTREPRISE
			// =========================
			Entreprise entreprise;
			Optional<Entreprise> entrepriseOpt = entrepriseRepository.findByEmailIgnoreCase("contact@vermeg.com");
			if (entrepriseOpt.isPresent()) {
				entreprise = entrepriseOpt.get();
			} else {
				Entreprise e = new Entreprise();
				e.setNom("Vermeg");
				e.setAdresse("Tunis");
				e.setEmail("contact@vermeg.com");
				e.setTelephone("70123456");
				e.setSecteurActivite("Informatique");
				entreprise = entrepriseRepository.save(e);
			}

			// =========================
			// FILIERE
			// =========================
			Filiere filiere;
			Optional<Filiere> filiereOpt = filiereRepository.findByNomIgnoreCase("Informatique");
			if (filiereOpt.isPresent()) {
				filiere = filiereOpt.get();
			} else {
				Filiere f = new Filiere();
				f.setNom("Informatique");
				filiere = filiereRepository.save(f);
			}

			// =========================
			// ADMIN
			// =========================
			findOrCreateSeedUser(
					utilisateurRepository,
					"ADMIN",
					Role.ADMINISTRATEUR,
					Utilisateur.class,
					"admin@test.com",
					null,
					"20000000",
					() -> Utilisateur.builder()
						.nom("Admin")
						.prenom("System")
						.email("admin@test.com")
						.telephone("20000000")
						.motDePasse(passwordEncoder.encode("admin123"))
						.actif(true)
						.role(Role.ADMINISTRATEUR)
						.build()
			);

			// =========================
			// STAGIAIRE
			// =========================
			Stagiaire stagiaire = findOrCreateSeedUser(
					utilisateurRepository,
					"STAGIAIRE",
					Role.STAGIAIRE,
					Stagiaire.class,
					"stagiaire@test.com",
					"MAT001",
					"24444444",
					() -> Stagiaire.builder()
						.nom("Sarra")
						.prenom("BenAli")
						.email("stagiaire@test.com")
						.telephone("24444444")
						.motDePasse(passwordEncoder.encode("123456"))
						.actif(true)
						.role(Role.STAGIAIRE)
						.matricule("MAT001")
						.niveau(3)
						.filiere(filiere)
						.build()
			);

			// =========================
			// ENCADRANT ACADEMIQUE
			// =========================
			EncadrantAcademique encadrantAcademique = findOrCreateSeedUser(
					utilisateurRepository,
					"ENCADRANT_ACADEMIQUE",
					Role.ENCADRANT_ACADEMIQUE,
					EncadrantAcademique.class,
					"acad@test.com",
					"ACAD001",
					"21111111",
					() -> EncadrantAcademique.builder()
						.nom("Nadia")
						.prenom("Prof")
						.email("acad@test.com")
						.telephone("21111111")
						.motDePasse(passwordEncoder.encode("123456"))
						.actif(true)
						.role(Role.ENCADRANT_ACADEMIQUE)
						.grade("Maitre Assistante")
						.matricule("ACAD001")
						.specialite("Genie logiciel")
						.build()
			);

			// =========================
			// ENCADRANT PROFESSIONNEL
			// =========================
			EncadrantProfessionnel encadrantProfessionnel = findOrCreateSeedUser(
					utilisateurRepository,
					"ENCADRANT_PROFESSIONNEL",
					Role.ENCADRANT_PROFESSIONNEL,
					EncadrantProfessionnel.class,
					"pro@test.com",
					null,
					"22222222",
					() -> EncadrantProfessionnel.builder()
						.nom("Ali")
						.prenom("Pro")
						.email("pro@test.com")
						.telephone("22222222")
						.motDePasse(passwordEncoder.encode("123456"))
						.actif(true)
						.role(Role.ENCADRANT_PROFESSIONNEL)
						.poste("Ingenieur")
						.service("IT")
						.entreprise(entreprise)
						.build()
			);

			// =========================
			// RESPONSABLE ENTREPRISE
			// =========================
			ResponsableEntreprise responsableEntreprise = findOrCreateSeedUser(
					utilisateurRepository,
					"RESPONSABLE_ENTREPRISE",
					Role.RESPONSABLE_ENTREPRISE,
					ResponsableEntreprise.class,
					"resp@test.com",
					null,
					"23333333",
					() -> ResponsableEntreprise.builder()
						.nom("Hedi")
						.prenom("Resp")
						.email("resp@test.com")
						.telephone("23333333")
						.motDePasse(passwordEncoder.encode("123456"))
						.actif(true)
						.role(Role.RESPONSABLE_ENTREPRISE)
						.poste("Manager")
						.service("RH")
						.entreprise(entreprise)
						.build()
			);

			// =========================
			// STAGE DE TEST
			// =========================
			if (stagiaire == null || encadrantAcademique == null || encadrantProfessionnel == null
					|| responsableEntreprise == null) {
				System.out.println("Initialisation du stage de test ignoree : un ou plusieurs utilisateurs de test existent deja avec un email, matricule ou telephone different.");
			} else {
				boolean stageExiste = stageRepository.findAll().stream()
						.anyMatch(s -> "Stage PFE Test".equalsIgnoreCase(s.getTitre()));

				if (!stageExiste) {
					Stage stage = new Stage();
					stage.setTitre("Stage PFE Test");
					stage.setDateDebut(LocalDate.of(2026, 1, 1));
					stage.setDateFin(LocalDate.of(2026, 3, 1));
					stage.setDuree(2);
					stage.setNbSemaine(8);
					stage.setNiveauSouhaite("Master");
					stage.setSujet("Systeme de gestion des stages");

					stage.setStatut(StatutStage.TERMINE);
					stage.setStatutSujet(StatutValidation.VALIDEE);

					stage.setEntreprise(entreprise);
					stage.setStagiaire(stagiaire);
					stage.setEncadrantAcademique(encadrantAcademique);
					stage.setEncadrantProfessionnel(encadrantProfessionnel);
					stage.setTuteurEntreprise(responsableEntreprise);
					stage.setSujetValidePar(encadrantAcademique);

					stageRepository.save(stage);
				}
			}

			System.out.println("Donnees initialisees avec succes");
		};
	}

	private static <T extends Utilisateur> T findOrCreateSeedUser(UtilisateurRepository utilisateurRepository,
	                                                              String label,
	                                                              Role expectedRole,
	                                                              Class<T> expectedType,
	                                                              String email,
	                                                              String matricule,
	                                                              String telephone,
	                                                              Supplier<T> factory) {
		Optional<Utilisateur> existing = findExistingSeedUser(utilisateurRepository, email, matricule, telephone);
		if (existing.isPresent()) {
			return castSeedUserOrSkip(existing.get(), expectedType, expectedRole, label);
		}

		try {
			return utilisateurRepository.save(factory.get());
		} catch (DataIntegrityViolationException ex) {
			System.out.println("Initialisation ignoree pour " + label
					+ " : utilisateur deja existant (email, matricule ou telephone). "
					+ getDatabaseMessage(ex));
			return findExistingSeedUser(utilisateurRepository, email, matricule, telephone)
					.map(user -> castSeedUserOrSkip(user, expectedType, expectedRole, label))
					.orElse(null);
		}
	}

	private static Optional<Utilisateur> findExistingSeedUser(UtilisateurRepository utilisateurRepository,
	                                                          String email,
	                                                          String matricule,
	                                                          String telephone) {
		Optional<Utilisateur> byEmail = hasText(email)
				? utilisateurRepository.findByNormalizedEmail(email.trim())
				: Optional.empty();
		if (byEmail.isPresent()) {
			return byEmail;
		}

		Optional<Utilisateur> byMatricule = hasText(matricule)
				? utilisateurRepository.findByMatricule(matricule.trim())
				: Optional.empty();
		if (byMatricule.isPresent()) {
			return byMatricule;
		}

		return hasText(telephone)
				? utilisateurRepository.findByTelephone(telephone.trim())
				: Optional.empty();
	}

	private static <T extends Utilisateur> T castSeedUserOrSkip(Utilisateur user,
	                                                            Class<T> expectedType,
	                                                            Role expectedRole,
	                                                            String label) {
		if (!expectedType.isInstance(user) || user.getRole() != expectedRole) {
			System.out.println("Initialisation ignoree pour " + label
					+ " : un utilisateur existe deja avec le meme email, matricule ou telephone"
					+ " mais avec un type ou role different. id=" + user.getId());
			return null;
		}

		return expectedType.cast(user);
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static String getDatabaseMessage(DataIntegrityViolationException ex) {
		Throwable cause = ex.getMostSpecificCause();
		if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) {
			return "";
		}
		return "Detail MySQL: " + cause.getMessage();
	}
}
