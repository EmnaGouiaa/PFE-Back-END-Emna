package fsegs.pfebackendemnagouuiaa;

import fsegs.pfebackendemnagouuiaa.entities.*;
import fsegs.pfebackendemnagouuiaa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Point d'entrée Spring Boot de l'API de gestion des stages universitaires (PFE).
 * <p>
 * <strong>Rôle :</strong> démarre le contexte applicatif, active la planification
 * ({@code @EnableScheduling}) et l'exécution asynchrone ({@code @EnableAsync})
 * pour les tâches différées (emails, notifications).
 * </p>
 * <p>
 * <strong>Responsabilités additionnelles :</strong> en environnement de développement,
 * le bean {@code CommandLineRunner init} injecte des données de démonstration
 * (entreprise, filière, utilisateurs par rôle, stage complet avec convention,
 * cahier, réunion finale et fiche d'évaluation signée) si elles n'existent pas déjà.
 * </p>
 * <p>
 * <strong>Relations :</strong> s'appuie sur les repositories JPA du package
 * {@code repository} et sur {@link org.springframework.security.crypto.password.PasswordEncoder}
 * pour les mots de passe de seed ; les données créées alimentent les écrans front
 * sans configuration manuelle.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
public class PfeBackEndEmnaGouuiaaApplication {

	/**
	 * Lance l'application Spring Boot.
	 *
	 * @param args arguments de ligne de commande (profils, port, etc.)
	 */
	public static void main(String[] args) {
		SpringApplication.run(PfeBackEndEmnaGouuiaaApplication.class, args);
	}

	@Bean
	CommandLineRunner init(
			UtilisateurRepository utilisateurRepository,
			PasswordEncoder passwordEncoder,
			EntrepriseRepository entrepriseRepository,
			FiliereRepository filiereRepository,
			StageRepository stageRepository,
			ConventionStageRepository conventionStageRepository,
			CahierStageRepository cahierStageRepository,
			FicheEvaluationRepository ficheEvaluationRepository,
			CritereEvaluationRepository critereEvaluationRepository,
			NoteAttribueeRepository noteAttribueeRepository
	) {
		return args -> {

			// =================================================================
			// REGLES METIER STRICTES — NE JAMAIS VIOLER
			// 1. Toute entreprise DOIT avoir au moins un ResponsableEntreprise
			// 2. Tout EncadrantProfessionnel et ResponsableEntreprise DOIT etre lie a une Entreprise
			// 3. Tout stage DOIT avoir : stagiaire + encadrantAcademique + encadrantProfessionnel + tuteurEntreprise
			// 4. FicheEvaluation INTERDITE pour un stage EN_COURS
			// 5. ReunionFinale INTERDITE pour un stage EN_COURS
			// =================================================================
			// EXEMPLE REEL DE VIOLATION : BIAT Digital (id=3) a ete creee en base
			// sans ResponsableEntreprise ni EncadrantProfessionnel, ce qui a genere
			// des stages sans encadrant professionnel. Cette entreprise a ete
			// supprimee manuellement. Ne jamais reproduire ce cas.
			// =================================================================

			// =================================================================
			// ENTREPRISE
			// =================================================================
			// ATTENTION : toute entreprise creee ici DOIT etre suivie immediatement
			// de la creation de son ResponsableEntreprise et de son EncadrantProfessionnel.
			// REGLE METIER : toute entreprise DOIT avoir au moins un
			// ResponsableEntreprise lie. Ne jamais creer une entreprise
			// sans creer immediatement son representant.
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

			// =================================================================
			// FILIERE
			// =================================================================
			Filiere filiere;
			Optional<Filiere> filiereOpt = filiereRepository.findByNomIgnoreCase("Informatique");
			if (filiereOpt.isPresent()) {
				filiere = filiereOpt.get();
			} else {
				Filiere f = new Filiere();
				f.setNom("Informatique");
				filiere = filiereRepository.save(f);
			}

			// =================================================================
			// ADMIN
			// =================================================================
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

			// =================================================================
			// RESPONSABLE DE STAGES
			// =================================================================
			ResponsableServiceStages responsableServiceStages = findOrCreateSeedUser(
					utilisateurRepository,
					"RESPONSABLE_STAGE",
					Role.RESPONSABLE_STAGE,
					ResponsableServiceStages.class,
					"responsable@test.com",
					null,
					"25555555",
					() -> {
						ResponsableServiceStages rss = new ResponsableServiceStages();
						rss.setNom("Ben Salem");
						rss.setPrenom("Kamel");
						rss.setEmail("responsable@test.com");
						rss.setTelephone("25555555");
						rss.setMotDePasse(passwordEncoder.encode("123456"));
						rss.setActif(true);
						rss.setSupprime(false);
						rss.setDoitChangerMotDePasse(false);
						rss.setRole(Role.RESPONSABLE_STAGE);
						rss.setService("Service des Stages");
						return rss;
					}
			);

			// =================================================================
			// STAGIAIRE
			// =================================================================
			final Filiere filiereFinale = filiere;
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
						.filiere(filiereFinale)
						.build()
			);

			// =================================================================
			// ENCADRANT ACADEMIQUE
			// =================================================================
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

			// =================================================================
			// ENCADRANT PROFESSIONNEL
			// =================================================================
			final Entreprise entrepriseFinale = entreprise;
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
						.entreprise(entrepriseFinale)
						.build()
			);
			if (encadrantProfessionnel == null) {
				System.out.println("[Seed] ERREUR : EncadrantProfessionnel null - impossible de creer un stage sans lui. Arret.");
				return;
			}

			// =================================================================
			// RESPONSABLE ENTREPRISE
			// =================================================================
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
						.entreprise(entrepriseFinale)
						.build()
			);
			if (responsableEntreprise == null) {
				System.out.println("[Seed] ERREUR : entreprise creee sans ResponsableEntreprise - seed invalide. Arret.");
				return;
			}

			// =================================================================
			// STAGE DE TEST — EN_COURS, sujet valide, tous encadrants affectes
			// =================================================================
			// REGLE METIER : un stage NE PEUT PAS etre cree sans encadrantProfessionnel, encadrantAcademique, stagiaire et tuteurEntreprise.
			if (stagiaire == null || encadrantAcademique == null || encadrantProfessionnel == null
					|| responsableEntreprise == null || responsableServiceStages == null) {
				System.out.println("[Seed] Stage ignore : un ou plusieurs utilisateurs obligatoires sont null (enc pro, enc acad, stagiaire, resp entreprise, rss).");
				System.out.println("Donnees initialisees avec succes");
				return;
			}

			// PATTERN OBLIGATOIRE : l'encadrant professionnel est cree et lie a l'entreprise
			// AVANT la creation du stage. Le stage recoit ensuite cet encadrant via
			// setEncadrantProfessionnel(). Ne jamais creer un stage sans avoir
			// prealablement cree et recupere l'encadrant professionnel de la meme entreprise.
			// EXEMPLE DE VIOLATION : BIAT Digital (id=3) avait un stage avec
			// encadrant_professionnel_id = NULL car l'enc pro n'avait pas ete cree
			// pour cette entreprise. Corrige par suppression manuelle en base.
			// Rechercher ou creer le stage
			Stage stage = stageRepository.findAll().stream()
					.filter(s -> "Stage PFE Test".equalsIgnoreCase(s.getTitre()))
					.findFirst()
					.orElse(null);

			if (stage == null) {
				Stage newStage = new Stage();
				newStage.setTitre("Stage PFE Test");
				newStage.setDateDebut(LocalDate.of(2026, 3, 1));
				newStage.setDateFin(LocalDate.of(2026, 6, 1));
				newStage.setDuree(3);
				newStage.setNbSemaine(12);
				newStage.setNiveauSouhaite("Master");
				newStage.setSujet("Systeme de gestion des stages universitaires");
				newStage.setStatut(StatutStage.EN_COURS);
				newStage.setStatutSujet(StatutValidation.VALIDEE);
				newStage.setEntreprise(entreprise);
				newStage.setStagiaire(stagiaire);
				newStage.setEncadrantAcademique(encadrantAcademique);
				newStage.setEncadrantProfessionnel(encadrantProfessionnel);
				newStage.setTuteurEntreprise(responsableEntreprise);
				newStage.setSujetValidePar(encadrantAcademique);
				newStage.setSectionEvaluationOuverte(false);
				if (newStage.getEncadrantProfessionnel() == null
						|| newStage.getEncadrantAcademique() == null
						|| newStage.getStagiaire() == null
						|| newStage.getTuteurEntreprise() == null) {
					System.out.println("[Seed] ERREUR : stage refuse - tous les acteurs obligatoires doivent etre renseignes (stagiaire, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise). Arret.");
					return;
				}
				stage = stageRepository.save(newStage);
				System.out.println("[Seed] Stage cree : id=" + stage.getId());
			} else {
				System.out.println("[Seed] Stage existant trouve : id=" + stage.getId());
			}

			final Long stageId          = stage.getId();
			final Long stagiaireId      = stagiaire.getId();
			final Long encAcadId        = encadrantAcademique.getId();
			final Long encProId         = encadrantProfessionnel.getId();
			final Long respEntId        = responsableEntreprise.getId();
			final Long respUniId        = responsableServiceStages.getId();

			// =================================================================
			// CONVENTION DE STAGE — 5 signatures (completement signee)
			// =================================================================
			if (!conventionStageRepository.existsByStageId(stageId)) {
				ConventionStage conv = new ConventionStage();
				conv.setNumConv(9001);
				conv.setDateDebut(LocalDate.of(2026, 3, 1));
				conv.setDateFin(LocalDate.of(2026, 6, 1));
				conv.setStage(stage);
				conv.setSignatures(new ArrayList<>(List.of(
						sig(RoleSignature.STAGIAIRE,                stagiaireId, "2026-03-03T09:00"),
						sig(RoleSignature.ENCADRANT_ACADEMIQUE,     encAcadId,   "2026-03-04T10:00"),
						sig(RoleSignature.ENCADRANT_PROFESSIONNEL,  encProId,    "2026-03-05T11:00"),
						sig(RoleSignature.RESPONSABLE_ENTREPRISE,   respEntId,   "2026-03-06T14:00"),
						sig(RoleSignature.RESPONSABLE_UNIVERSITAIRE,respUniId,   "2026-03-07T09:30")
				)));
				conventionStageRepository.save(conv);
				System.out.println("[Seed] Convention de stage creee (5 signatures).");
			} else {
				System.out.println("[Seed] Convention de stage deja presente.");
			}

			// =================================================================
			// CAHIER DE STAGE — 4 signatures (completement signe)
			// =================================================================
			if (!cahierStageRepository.existsByStageId(stageId)) {
				CahierStage cahier = new CahierStage();
				cahier.setDateGeneration(LocalDate.of(2026, 3, 1));
				cahier.setDateSignature(LocalDate.of(2026, 3, 10));
				cahier.setStage(stage);
				cahier.setSignatures(new ArrayList<>(List.of(
						sig(RoleSignature.STAGIAIRE,               stagiaireId, "2026-03-10T08:30"),
						sig(RoleSignature.ENCADRANT_ACADEMIQUE,    encAcadId,   "2026-03-10T09:00"),
						sig(RoleSignature.ENCADRANT_PROFESSIONNEL, encProId,    "2026-03-10T10:00"),
						sig(RoleSignature.RESPONSABLE_ENTREPRISE,  respEntId,   "2026-03-10T11:00")
				)));
				cahierStageRepository.save(cahier);
				System.out.println("[Seed] Cahier de stage cree (4 signatures).");
			} else {
				System.out.println("[Seed] Cahier de stage deja present.");
			}

			// Fiche d'evaluation : non creee en seed pour un stage EN_COURS (regle metier).
			System.out.println("[Seed] Fiche d'evaluation non creee — stage encore EN_COURS.");

			System.out.println("Donnees initialisees avec succes");
		};
	}

	// =========================================================================
	// Helpers statiques
	// =========================================================================

	/** Cree une {@link Signature} de seed sans l'enregistrer (cascade via le document parent). */
	private static Signature sig(RoleSignature role, Long signataireId, String isoDateTime) {
		Signature s = new Signature();
		s.setRoleSignature(role);
		s.setSignataireId(signataireId);
		s.setDateSignature(LocalDateTime.parse(isoDateTime));
		return s;
	}

	/** Cree et enregistre un {@link CritereEvaluation} lie a une fiche. */
	private static CritereEvaluation saveCritere(CritereEvaluationRepository repo,
	                                              FicheEvaluation fiche,
	                                              String libelle,
	                                              String description,
	                                              PartieEvaluation partie,
	                                              int bareme) {
		CritereEvaluation c = new CritereEvaluation();
		c.setLibelle(libelle);
		c.setDescription(description);
		c.setCategorie("Evaluation du stage");
		c.setPartie(partie);
		c.setBareme(bareme);
		c.setConsigne("Attribuer une note entre 0 et " + bareme + ".");
		c.setFiche(fiche);
		return repo.save(c);
	}

	/** Cree et enregistre une {@link NoteAttribuee} avec sa cle composite. */
	private static void saveNote(NoteAttribueeRepository repo,
	                             FicheEvaluation fiche,
	                             CritereEvaluation critere,
	                             int poids,
	                             int bareme,
	                             int note,
	                             String commentaire) {
		NoteAttribuee na = new NoteAttribuee();
		// La cle composite doit etre initialisee explicitement (IDs deja generes par la BD)
		na.setId(new CleNoteAttribuee(fiche.getId(), critere.getId()));
		na.setFicheEvaluation(fiche);
		na.setCritereEvaluation(critere);
		na.setPoids(poids);
		na.setBareme(bareme);
		na.setNote(note);
		na.setCommentaire(commentaire);
		repo.save(na);
	}

	// =========================================================================
	// Utilitaire de creation/recherche d'utilisateur de seed
	// =========================================================================

	private static <T extends Utilisateur> T findOrCreateSeedUser(
			UtilisateurRepository utilisateurRepository,
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
		if (byEmail.isPresent()) return byEmail;

		Optional<Utilisateur> byMatricule = hasText(matricule)
				? utilisateurRepository.findByMatricule(matricule.trim())
				: Optional.empty();
		if (byMatricule.isPresent()) return byMatricule;

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
		if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) return "";
		return "Detail MySQL: " + cause.getMessage();
	}
}
