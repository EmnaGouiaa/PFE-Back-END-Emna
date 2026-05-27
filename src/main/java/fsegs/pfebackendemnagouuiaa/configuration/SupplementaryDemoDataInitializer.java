package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.DemandeCreationCompteEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.OffreStage;
import fsegs.pfebackendemnagouuiaa.entities.PartieEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutOffre;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.CritereEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.DemandeCreationCompteEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.OffreStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Initialise les tables complémentaires non couvertes par {@link DemoDataInitializer}.
 *
 * <p>Tables initialisées par ce runner :
 * <ol>
 *   <li>{@code offre_stage}          — offres publiées pour chaque entreprise demo</li>
 *   <li>{@code critere_evaluation}   — gabarits globaux de critères d'évaluation</li>
 *   <li>{@code cahier_stage}         — cahier de bord pour chaque stage actif</li>
 *   <li>{@code reunion} (FINALE)     — réunion finale automatique par stage</li>
 *   <li>{@code fiche_evaluation}     — fiche vide pour les stages TERMINÉS</li>
 *   <li>{@code conventions_stage}    — convention pour les stages EN_COURS / TERMINÉS</li>
 *   <li>{@code demandes_stage}       — demandes de création de compte entreprise</li>
 * </ol>
 *
 * <p>Ordre d'exécution :
 * <pre>
 *   @Order(0)   SchemaCompatibilityRunner
 *   @Order(50)  DemoDataInitializer          ← crée filieres, entreprises, users, stages
 *   @Order(60)  SupplementaryDemoDataInitializer  ← CE RUNNER
 * </pre>
 *
 * <p><b>IDEMPOTENT</b> : chaque bloc vérifie l'existence avant d'insérer.
 * Redémarrer l'application ne crée pas de doublons.
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class SupplementaryDemoDataInitializer implements CommandLineRunner {

    // ─── Repositories ────────────────────────────────────────────────────────
    private final StageRepository                           stageRepository;
    private final EntrepriseRepository                      entrepriseRepository;
    private final ResponsableEntrepriseRepository           responsableEntrepriseRepository;
    private final StagiaireRepository                       stagiaireRepository;
    private final OffreStageRepository                      offreStageRepository;
    private final DemandeCreationCompteEntrepriseRepository demandeRepository;
    private final CritereEvaluationRepository               critereEvaluationRepository;
    private final ConventionStageRepository                 conventionStageRepository;
    private final CahierStageRepository                     cahierStageRepository;
    private final ReunionFinaleRepository                   reunionFinaleRepository;
    private final FicheEvaluationRepository                 ficheEvaluationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void run(String... args) {
        log.info("[SUPP-INIT] ===== Début de l'initialisation complémentaire =====");

        initialiserOffresStage();           // étape 1
        initialiserCriteresEvaluation();    // étape 2
        initialiserDocumentsStages();       // étapes 3-6
        initialiserDemandesEntreprise();    // étape 7

        log.info("[SUPP-INIT] ===== Fin de l'initialisation complémentaire =====");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ÉTAPE 1 — OFFRES DE STAGE
    // ═════════════════════════════════════════════════════════════════════════

    private void initialiserOffresStage() {
        Entreprise telnet   = entrepriseRepository.findByNomIgnoreCase("Telnet Tunisie").orElse(null);
        Entreprise sofrecom = entrepriseRepository.findByNomIgnoreCase("Sofrecom Tunisie").orElse(null);
        Entreprise biat     = entrepriseRepository.findByNomIgnoreCase("BIAT Digital").orElse(null);

        if (telnet == null || sofrecom == null || biat == null) {
            log.warn("[SUPP-INIT] Entreprises demo introuvables — offres de stage ignorées.");
            return;
        }

        // Responsables : optionnels (pas de contrainte NOT NULL sur publiee_par_id)
        ResponsableEntreprise reTelnet = responsableEntrepriseRepository
                .findByEntrepriseId(telnet.getId()).stream().findFirst().orElse(null);
        ResponsableEntreprise reSofrecom = responsableEntrepriseRepository
                .findByEntrepriseId(sofrecom.getId()).stream().findFirst().orElse(null);
        ResponsableEntreprise reBiat = responsableEntrepriseRepository
                .findByEntrepriseId(biat.getId()).stream().findFirst().orElse(null);

        // Dates futures relatives a aujourd'hui — garantit que les offres demarrent
        // toujours dans le futur, quel que soit le moment d'execution du seed.
        LocalDate base = LocalDate.now().plusMonths(2);

        // ── Telnet Tunisie ────────────────────────────────────────────────────
        if (offreStageRepository.findByEntrepriseId(telnet.getId()).isEmpty()) {
            sauvegarderOffre(
                    "Développeur Full Stack Java / Angular",
                    "Conception et développement de modules applicatifs en Java Spring Boot et Angular 17. "
                    + "Participation aux cérémonies Agile, revue de code et mise en production sur les "
                    + "environnements de recette et de production.",
                    3,
                    "Etudiant Bac+3 Informatique ou Génie Logiciel, maîtrise de Java et Angular requise.",
                    base, StatutOffre.EN_ATTENTE, telnet, reTelnet);

            sauvegarderOffre(
                    "Ingénieur DevOps / CI-CD",
                    "Mise en place d'un pipeline CI/CD complet avec Jenkins, Docker et Kubernetes dans un "
                    + "environnement cloud hybride. Automatisation des déploiements et monitoring des services.",
                    4,
                    "Profil DevOps, maîtrise de Linux, Docker et Git. Connaissance d'au moins un outil CI/CD.",
                    base.plusDays(15), StatutOffre.EN_ATTENTE, telnet, reTelnet);

            log.info("[SUPP-INIT] 2 offres de stage créées pour Telnet Tunisie (EN_ATTENTE).");
        }

        // ── Sofrecom Tunisie ──────────────────────────────────────────────────
        if (offreStageRepository.findByEntrepriseId(sofrecom.getId()).isEmpty()) {
            sauvegarderOffre(
                    "Analyste Réseau et Sécurité Informatique",
                    "Audit de sécurité des infrastructures réseau, analyse des vulnérabilités et déploiement "
                    + "de correctifs. Rédaction de rapports de sécurité à destination des équipes techniques.",
                    3,
                    "Etudiant Réseaux et Télécommunications, connaissance des protocoles TCP/IP et outils de sécurité.",
                    base.plusDays(5), StatutOffre.EN_ATTENTE, sofrecom, reSofrecom);

            sauvegarderOffre(
                    "Développeur Infrastructure Cloud",
                    "Déploiement et supervision d'infrastructures cloud sur AWS / Azure. "
                    + "Automatisation des environnements via Terraform et Ansible.",
                    4,
                    "Etudiant Informatique, expérience Linux et au moins une plateforme cloud (AWS, Azure, GCP).",
                    base.plusDays(30), StatutOffre.EN_ATTENTE, sofrecom, reSofrecom);

            log.info("[SUPP-INIT] 2 offres de stage créées pour Sofrecom Tunisie (EN_ATTENTE).");
        }

        // ── BIAT Digital ──────────────────────────────────────────────────────
        if (offreStageRepository.findByEntrepriseId(biat.getId()).isEmpty()) {
            sauvegarderOffre(
                    "Développeur API Bancaire REST",
                    "Développement et sécurisation d'une API REST pour les services bancaires mobiles. "
                    + "Intégration OAuth2, journalisation des accès sensibles et tests de charge.",
                    4,
                    "Etudiant Génie Logiciel ou Informatique, maîtrise de Spring Boot et notions de sécurité applicative.",
                    base.plusDays(20), StatutOffre.EN_ATTENTE, biat, reBiat);

            sauvegarderOffre(
                    "Data Analyst / Business Intelligence",
                    "Conception de tableaux de bord BI pour le suivi des indicateurs financiers. "
                    + "Collecte et transformation de données depuis des sources hétérogènes (SQL, API, Excel).",
                    3,
                    "Etudiant Informatique ou Statistiques, connaissance de Power BI ou Tableau et maîtrise de SQL.",
                    base.plusDays(40), StatutOffre.EN_ATTENTE, biat, reBiat);

            log.info("[SUPP-INIT] 2 offres de stage créées pour BIAT Digital (EN_ATTENTE).");
        }
    }

    /**
     * Construit et persiste une {@link OffreStage} dans son ETAT INITIAL — EN_ATTENTE.
     *
     * <p>Aucune date de publication ni de validation : l'offre est soumise par
     * le responsable entreprise et doit etre approuvee par le responsable des stages
     * universitaires. Le workflow d'approbation se deroulera ensuite normalement via
     * l'IHM (validerOffre / refuserOffre).</p>
     */
    private void sauvegarderOffre(String titre, String description, int dureeMois, String profil,
                                   LocalDate dateDebut, StatutOffre statut,
                                   Entreprise entreprise, ResponsableEntreprise publiePar) {
        OffreStage offre = new OffreStage();
        offre.setTitre(titre);
        offre.setDescriptionMissions(description);
        offre.setDuree(dureeMois);
        offre.setProfilRecherche(profil);
        offre.setDateDebutPrevue(dateDebut);
        offre.setDatePublication(null);     // pas encore publiee (statut EN_ATTENTE)
        offre.setStatut(statut);
        offre.setEntreprise(entreprise);    // NOT NULL — toujours renseigne
        offre.setPublieePar(publiePar);     // nullable
        offreStageRepository.save(offre);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ÉTAPE 2 — CRITÈRES D'ÉVALUATION (gabarits globaux, fiche = null)
    // ═════════════════════════════════════════════════════════════════════════

    private void initialiserCriteresEvaluation() {
        if (critereEvaluationRepository.count() > 0) {
            log.info("[SUPP-INIT] Critères d'évaluation déjà présents — étape ignorée.");
            return;
        }

        // ── Partie ENCADRANT_PROFESSIONNEL (barème cible : 100 pts) ──────────
        sauvegarderCritere("Maîtrise technique",
                "Niveau de compétence technique et professionnelle démontré pendant le stage.",
                "Compétences Techniques", 20, PartieEvaluation.ENCADRANT_PROFESSIONNEL);

        sauvegarderCritere("Autonomie et initiative",
                "Capacité à travailler de manière autonome et à proposer des solutions pertinentes "
                + "sans attendre d'instructions systématiques.",
                "Compétences Professionnelles", 15, PartieEvaluation.ENCADRANT_PROFESSIONNEL);

        sauvegarderCritere("Qualité du travail fourni",
                "Rigueur, précision, respect des normes et des standards de qualité internes de l'entreprise.",
                "Qualité", 20, PartieEvaluation.ENCADRANT_PROFESSIONNEL);

        sauvegarderCritere("Respect des délais",
                "Ponctualité dans la réalisation des tâches et respect du planning convenu.",
                "Organisation", 15, PartieEvaluation.ENCADRANT_PROFESSIONNEL);

        sauvegarderCritere("Communication et esprit d'équipe",
                "Qualité des échanges avec les collègues, l'encadrant et la hiérarchie. "
                + "Capacité à travailler en équipe.",
                "Soft Skills", 15, PartieEvaluation.ENCADRANT_PROFESSIONNEL);

        sauvegarderCritere("Adaptabilité à l'environnement professionnel",
                "Capacité d'adaptation aux outils, méthodes, processus et culture de l'entreprise.",
                "Soft Skills", 15, PartieEvaluation.ENCADRANT_PROFESSIONNEL);

        // ── Partie RESPONSABLE_ENTREPRISE (barème cible : 100 pts) ───────────
        sauvegarderCritere("Présentation et comportement professionnel",
                "Tenue vestimentaire, ponctualité quotidienne et attitude générale au sein de l'entreprise.",
                "Comportement", 20, PartieEvaluation.RESPONSABLE_ENTREPRISE);

        sauvegarderCritere("Implication et motivation",
                "Niveau d'engagement, d'enthousiasme et de proactivité observé tout au long du stage.",
                "Comportement", 20, PartieEvaluation.RESPONSABLE_ENTREPRISE);

        sauvegarderCritere("Qualité du rapport de stage",
                "Pertinence, clarté, richesse et qualité rédactionnelle du rapport final rendu.",
                "Production Écrite", 30, PartieEvaluation.RESPONSABLE_ENTREPRISE);

        sauvegarderCritere("Atteinte des objectifs fixés",
                "Degré d'accomplissement des missions et des objectifs définis en début de stage.",
                "Résultats", 30, PartieEvaluation.RESPONSABLE_ENTREPRISE);

        log.info("[SUPP-INIT] 10 critères d'évaluation globaux créés (6 EncadrantPro + 4 ResponsableEntreprise).");
    }

    /** Construit et persiste un {@link CritereEvaluation} sans lien à une fiche (gabarit global). */
    private void sauvegarderCritere(String libelle, String description, String categorie,
                                     int bareme, PartieEvaluation partie) {
        CritereEvaluation c = new CritereEvaluation();
        c.setLibelle(libelle);
        c.setDescription(description);
        c.setCategorie(categorie);
        c.setBareme(bareme);
        c.setPartie(partie);
        // fiche = null → gabarit global réutilisable, non lié à une fiche spécifique
        critereEvaluationRepository.save(c);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ÉTAPES 3-6 — DOCUMENTS PAR STAGE
    //   Ordre intra-stage : CahierStage → ReunionFinale → FicheEvaluation → ConventionStage
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Pre-creation des documents de stage DESACTIVEE.
     *
     * <p>Conformement aux regles metier, l'etat initial du systeme ne doit contenir
     * NI CahierStage, NI ReunionFinale, NI FicheEvaluation, NI ConventionStage
     * pre-genere :</p>
     *
     * <ul>
     *   <li>La <b>ConventionStage</b> est generee automatiquement par le service Stage
     *       lors de la validation du sujet par l'encadrant academique
     *       ({@code StageServiceImpl.genererConventionSiAbsente}).</li>
     *   <li>Le <b>CahierStage</b> et la <b>ReunionFinale</b> sont crees au declenchement
     *       du stage (transition EN_COURS).</li>
     *   <li>La <b>FicheEvaluation</b> apparait uniquement en fin de stage, jamais en seed.</li>
     * </ul>
     *
     * <p>Garder ces objets en seed donnerait l'impression d'un stage deja en cours et
     * empecherait de tester le workflow de bout en bout. Methode conservee comme
     * placeholder documente — peut etre supprimee ulterieurement.</p>
     */
    private void initialiserDocumentsStages() {
        log.info("[SUPP-INIT] Pre-creation des documents stages volontairement DESACTIVEE "
                + "(etat initial frais — chaque document est cree par le workflow reel).");
    }

    /**
     * Crée un {@link CahierStage} vide pour le stage s'il n'en a pas encore.
     *
     * @return {@code true} si un nouveau cahier a été persisté.
     */
    private boolean creerCahierStageIfAbsent(Stage stage) {
        if (cahierStageRepository.existsByStageId(stage.getId())) {
            return false;
        }
        CahierStage cahier = new CahierStage();
        cahier.setStage(stage);
        cahier.setDateGeneration(LocalDate.now());
        cahierStageRepository.save(cahier);
        return true;
    }

    /**
     * Crée la {@link ReunionFinale} automatique du stage si elle n'existe pas encore.
     * Les participants sont laissés vides : ils sont résolus dynamiquement par le service
     * lors de la prochaine mise à jour du stage.
     *
     * @return la réunion finale créée ou déjà existante, {@code null} si {@code dateFin} absente.
     */
    private ReunionFinale creerReunionFinaleIfAbsent(Stage stage) {
        if (reunionFinaleRepository.existsByStageId(stage.getId())) {
            return reunionFinaleRepository.findFirstByStageIdOrderByIdAsc(stage.getId()).orElse(null);
        }

        if (stage.getDateFin() == null) {
            log.warn("[SUPP-INIT] Stage id={} sans dateFin — ReunionFinale ignorée.", stage.getId());
            return null;
        }

        // Lier au CahierStage s'il vient d'être créé ou existait déjà
        CahierStage cahier = cahierStageRepository.findByStageId(stage.getId()).orElse(null);

        ReunionFinale rf = new ReunionFinale();
        rf.setStage(stage);
        rf.setCahierStage(cahier);
        rf.setNumReunion("RF-" + stage.getId());
        rf.setDate(stage.getDateFin());
        rf.setHeure(LocalTime.of(9, 0));
        rf.setTypeEncadrantCreateur("SYSTEME");
        rf.setNomEncadrantCreateur("SYSTEME");
        rf.setParticipants(new LinkedHashSet<>());   // vide — complété par le service au prochain save

        return reunionFinaleRepository.save(rf);
    }

    /**
     * Crée une {@link FicheEvaluation} vide pour le stage si elle n'existe pas encore.
     * Seuls les champs obligatoires (pointsForts, axes, note) sont initialisés à vide.
     *
     * @return {@code true} si une nouvelle fiche a été persistée.
     */
    private boolean creerFicheEvaluationIfAbsent(Stage stage, ReunionFinale reunionFinale) {
        if (ficheEvaluationRepository.existsByStageId(stage.getId())) {
            return false;
        }
        FicheEvaluation fiche = new FicheEvaluation();
        fiche.setStage(stage);
        fiche.setReunionFinale(reunionFinale);
        // Champs obligatoires initialisés à vide — remplis lors de l'évaluation réelle
        fiche.setPointFortEncadrantPro("");
        fiche.setAxeAmeliorationEncadrantPro("");
        fiche.setPointFortResponsableEntreprise("");
        fiche.setAxeAmeliorationResponsableEntreprise("");
        fiche.setNoteFinale(0.0);
        ficheEvaluationRepository.save(fiche);
        return true;
    }

    /**
     * Crée une {@link ConventionStage} non signée pour le stage si elle n'existe pas encore.
     * Le numéro de convention ({@code numConv}) est basé sur l'ID du stage (garanti unique).
     *
     * @return {@code true} si une nouvelle convention a été persistée.
     */
    private boolean creerConventionStageIfAbsent(Stage stage) {
        if (conventionStageRepository.existsByStageId(stage.getId())) {
            return false;
        }
        ConventionStage convention = new ConventionStage();
        convention.setStage(stage);
        convention.setDateDebut(stage.getDateDebut());
        convention.setDateFin(stage.getDateFin());
        convention.setNumConv(stage.getId().intValue());  // unique car basé sur l'ID du stage
        conventionStageRepository.save(convention);
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ÉTAPE 7 — DEMANDES DE CRÉATION DE COMPTE ENTREPRISE
    // ═════════════════════════════════════════════════════════════════════════

    private void initialiserDemandesEntreprise() {

        // ── Demande 1 : Sara Abidi (STG-004) → Satoriplex Systems ───────────
        if (!demandeRepository.existsByEmailEntreprise("contact@satoriplex.tn")) {
            stagiaireRepository.findByMatricule("STG-004").ifPresent(sara -> {
                DemandeCreationCompteEntreprise demande = DemandeCreationCompteEntreprise.builder()
                        .stagiaire(sara)
                        .nomEntreprise("Satoriplex Systems")
                        .emailEntreprise("contact@satoriplex.tn")
                        .telephoneEntreprise("+21671200001")
                        .adresse("Cité Olympique, Ariana, Tunis")
                        .secteurActivite("Technologies de l'Information")
                        .nomResponsable("مزوغي")
                        .prenomResponsable("رامي")
                        .emailResponsable("rami.mzoughi@satoriplex.tn")
                        .telephoneResponsable("+21698300001")
                        .build();
                demandeRepository.save(demande);
                log.info("[SUPP-INIT] Demande 'Satoriplex Systems' créée pour STG-004 (Sara Abidi).");
            });
        }

        // ── Demande 2 : Mohamed Bouaziz (STG-005) → DataSoft Tunisia ─────────
        if (!demandeRepository.existsByEmailEntreprise("contact@datasoft.tn")) {
            stagiaireRepository.findByMatricule("STG-005").ifPresent(mohamed -> {
                DemandeCreationCompteEntreprise demande = DemandeCreationCompteEntreprise.builder()
                        .stagiaire(mohamed)
                        .nomEntreprise("DataSoft Tunisia")
                        .emailEntreprise("contact@datasoft.tn")
                        .telephoneEntreprise("+21671200002")
                        .adresse("Avenue Fattouma Bourguiba, Manouba")
                        .secteurActivite("Logiciels et Services Informatiques")
                        .nomResponsable("براهمي")
                        .prenomResponsable("ليلى")
                        .emailResponsable("leila.brahmi@datasoft.tn")
                        .telephoneResponsable("+21698300002")
                        .build();
                demandeRepository.save(demande);
                log.info("[SUPP-INIT] Demande 'DataSoft Tunisia' créée pour STG-005 (Mohamed Bouaziz).");
            });
        }

        // ── Demande 3 : Youssef Hamdi (STG-003) → InnoCom Solutions ──────────
        if (!demandeRepository.existsByEmailEntreprise("contact@innocom.tn")) {
            stagiaireRepository.findByMatricule("STG-003").ifPresent(youssef -> {
                DemandeCreationCompteEntreprise demande = DemandeCreationCompteEntreprise.builder()
                        .stagiaire(youssef)
                        .nomEntreprise("InnoCom Solutions")
                        .emailEntreprise("contact@innocom.tn")
                        .telephoneEntreprise("+21671200003")
                        .adresse("Les Berges du Lac II, Tunis")
                        .secteurActivite("Conseil et Intégration de Systèmes")
                        .nomResponsable("الشارني")
                        .prenomResponsable("خالد")
                        .emailResponsable("khaled.charni@innocom.tn")
                        .telephoneResponsable("+21698300003")
                        .build();
                demandeRepository.save(demande);
                log.info("[SUPP-INIT] Demande 'InnoCom Solutions' créée pour STG-003 (Youssef Hamdi).");
            });
        }
    }
}
