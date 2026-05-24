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

@SpringBootApplication
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
public class PfeBackEndEmnaGouuiaaApplication {

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
			ReunionFinaleRepository reunionFinaleRepository,
			FicheEvaluationRepository ficheEvaluationRepository,
			CritereEvaluationRepository critereEvaluationRepository,
			NoteAttribueeRepository noteAttribueeRepository
	) {
		return args -> {

			// =================================================================
			// ENTREPRISE
			// =================================================================
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

			// =================================================================
			// STAGE DE TEST — EN_COURS, sujet valide, tous encadrants affectes
			// =================================================================
			if (stagiaire == null || encadrantAcademique == null || encadrantProfessionnel == null
					|| responsableEntreprise == null || responsableServiceStages == null) {
				System.out.println("[Seed] Stage ignore : un ou plusieurs utilisateurs de test sont indisponibles.");
				System.out.println("Donnees initialisees avec succes");
				return;
			}

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
				newStage.setSectionEvaluationOuverte(true);
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

			// =================================================================
			// REUNION FINALE
			// =================================================================
			ReunionFinale reunionFinale;
			if (!reunionFinaleRepository.existsByStageId(stageId)) {
				ReunionFinale rf = new ReunionFinale();
				rf.setNumReunion("RF-001");
				rf.setDate(LocalDate.of(2026, 5, 28));
				rf.setHeure(LocalTime.of(10, 0));
				rf.setStage(stage);
				rf.setCompteRendu(
						"Soutenance de fin de stage realisee avec succes. " +
						"Le stagiaire a presente son travail de maniere claire et structuree. " +
						"Bonne maitrise du sujet et des technologies utilisees."
				);
				rf.setNomEncadrantCreateur("Nadia Prof");
				rf.setTypeEncadrantCreateur("ENCADRANT_ACADEMIQUE");
				rf.setEncadrantCreateurId(encAcadId);
				reunionFinale = reunionFinaleRepository.save(rf);
				System.out.println("[Seed] Reunion finale creee : id=" + reunionFinale.getId());
			} else {
				reunionFinale = reunionFinaleRepository
						.findFirstByStageIdOrderByIdAsc(stageId)
						.orElse(null);
				System.out.println("[Seed] Reunion finale deja presente.");
			}

			// =================================================================
			// FICHE D'EVALUATION — criteres + notes + 2 signatures (verrouillee)
			// =================================================================
			if (!ficheEvaluationRepository.existsByStageId(stageId) && reunionFinale != null) {

				// ── Fiche (sans notes ni criteres pour l'instant) ─────────────
				FicheEvaluation fiche = new FicheEvaluation();
				fiche.setStage(stage);
				fiche.setReunionFinale(reunionFinale);
				fiche.setPointFortEncadrantPro(
						"Excellente maitrise des technologies Java / Spring Boot. " +
						"Grande autonomie et forte motivation tout au long du stage."
				);
				fiche.setAxeAmeliorationEncadrantPro(
						"Travailler la communication ecrite et la redaction de documentation technique."
				);
				fiche.setPointFortResponsableEntreprise(
						"Integration remarquable dans l'equipe. Respect rigoureux des delais."
				);
				fiche.setAxeAmeliorationResponsableEntreprise(
						"Approfondir les competences en gestion de projet agile."
				);

				// 2 signatures obligatoires (EP + RE) → fiche verrouillee
				fiche.setSignatures(new ArrayList<>(List.of(
						sig(RoleSignature.ENCADRANT_PROFESSIONNEL, encProId,  "2026-05-28T14:00"),
						sig(RoleSignature.RESPONSABLE_ENTREPRISE,  respEntId, "2026-05-28T15:30")
				)));

				fiche = ficheEvaluationRepository.save(fiche);

				// ── Criteres d'evaluation ────────────────────────────────────
				// Regle metier : somme des poids = 20 (note finale sur 20)
				// 3 criteres EP (poids 4 chacun = 12) + 2 criteres RE (poids 4 chacun = 8) = 20 ✓
				// Bareme : 5 points par critere

				CritereEvaluation c1 = saveCritere(critereEvaluationRepository, fiche,
						"Competences techniques",
						"Maitrise des outils, langages et frameworks utilises durant le stage.",
						PartieEvaluation.ENCADRANT_PROFESSIONNEL, 5);

				CritereEvaluation c2 = saveCritere(critereEvaluationRepository, fiche,
						"Autonomie et initiative",
						"Capacite a identifier les problemes et proposer des solutions sans sollicitation permanente.",
						PartieEvaluation.ENCADRANT_PROFESSIONNEL, 5);

				CritereEvaluation c3 = saveCritere(critereEvaluationRepository, fiche,
						"Communication et comportement professionnel",
						"Qualite des interactions avec l'equipe, respect des regles et ponctualite.",
						PartieEvaluation.ENCADRANT_PROFESSIONNEL, 5);

				CritereEvaluation c4 = saveCritere(critereEvaluationRepository, fiche,
						"Integration dans l'equipe",
						"Adaptation a la culture d'entreprise et collaboration avec les collegues.",
						PartieEvaluation.RESPONSABLE_ENTREPRISE, 5);

				CritereEvaluation c5 = saveCritere(critereEvaluationRepository, fiche,
						"Respect des delais et des consignes",
						"Livraison dans les temps et conformite aux specifications demandees.",
						PartieEvaluation.RESPONSABLE_ENTREPRISE, 5);

				// ── Notes attribuees ─────────────────────────────────────────
				// noteFinale = Σ (note/bareme)*poids
				//   c1 : (4/5)*4 = 3.20
				//   c2 : (4/5)*4 = 3.20
				//   c3 : (5/5)*4 = 4.00
				//   c4 : (4/5)*4 = 3.20
				//   c5 : (5/5)*4 = 4.00
				//   Total = 17.60 / 20

				saveNote(noteAttribueeRepository, fiche, c1, 4, 5, 4,
						"Tres bonne maitrise de Spring Boot et d'Angular.");
				saveNote(noteAttribueeRepository, fiche, c2, 4, 5, 4,
						"Travaille frequemment sans supervision directe.");
				saveNote(noteAttribueeRepository, fiche, c3, 4, 5, 5,
						"Communication exemplaire, relation excellente avec l'equipe.");
				saveNote(noteAttribueeRepository, fiche, c4, 4, 5, 4,
						"S'est integre tres rapidement dans l'equipe RH.");
				saveNote(noteAttribueeRepository, fiche, c5, 4, 5, 5,
						"Toutes les livraisons ont ete effectuees dans les delais prevus.");

				// Persistance de la note finale calculee
				fiche.setNoteFinale(17.60);
				ficheEvaluationRepository.save(fiche);

				System.out.println("[Seed] Fiche d'evaluation creee (5 criteres, note finale=17.60/20, 2 signatures).");
			} else {
				System.out.println("[Seed] Fiche d'evaluation deja presente ou reunion finale absente.");
			}

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
