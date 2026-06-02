package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.Filiere;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.AbsenceRepository;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FiliereRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableServiceStagesRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Initialise la base de données avec un jeu de données de démonstration cohérent et réaliste.
 *
 * <p><b>Rôle :</b> peupler filières, entreprises, tous les profils utilisateurs et cinq stages
 * en état initial « frais » pour tester le workflow complet depuis zéro.</p>
 *
 * <p><b>Idempotence :</b> vérifie l'existence (e-mail, matricule, titre de stage) avant chaque
 * insertion — aucune duplication au redémarrage.</p>
 *
 * <p><b>Scenario fin de stage :</b> etape 9 — {@value #SCENARIO_FIN_STAGE_TITRE} et
 * {@value #SCENARIO_FIN_STAGE_2_TITRE} chez {@value #ENTREPRISE_DEMO_FIN_NOM} ;
 * dates fixes {@code 28/02/2026} → {@code 31/05/2026}, convention, cahier, reunions et absences
 * (sans signatures, fiche, enquete ni reunion finale).</p>
 *
 * <p><b>Ordre d'exécution :</b></p>
 * <ol>
 *   <li>{@code @Order(0)} — {@link SchemaCompatibilityRunner}</li>
 *   <li>{@code @Order(50)} — ce runner</li>
 *   <li>{@code @Order(60)} — {@link SupplementaryDemoDataInitializer}</li>
 * </ol>
 *
 * <p><b>Relations :</b> utilise {@link org.springframework.security.crypto.password.PasswordEncoder}
 * (BCrypt), les repositories JPA du domaine stage/utilisateur ; mot de passe demo commun
 * {@value #DEMO_PASSWORD}.</p>
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    // -----------------------------------------------------------------------
    // Mot de passe commun a tous les comptes de demonstration
    // -----------------------------------------------------------------------
    private static final String DEMO_PASSWORD = "admin123*";

    /** Image PNG 1x1 pour permettre les signatures document en demonstration. */
    private static final String DEMO_URL_SIGNATURE =
            fsegs.pfebackendemnagouuiaa.services.DemoSigningSupport.DEMO_SIGNATURE_DATA_URL;

    /** Titre du 1er stage « fin de stage » — idempotent par stagiaire + titre. */
    private static final String SCENARIO_FIN_STAGE_TITRE =
            "[DEMO-FIN] Plateforme de gestion de stages — scenario fin de stage";

    /** Titre du 2e stage isole (meme entreprise / encadrants, autre stagiaire). */
    private static final String SCENARIO_FIN_STAGE_2_TITRE =
            "[DEMO-FIN-2] Plateforme de gestion de stages — scenario fin de stage (stagiaire B)";

    private static final String ENTREPRISE_DEMO_FIN_NOM = "TechCorp Tunisia";

    /** Dates fixes du scenario [DEMO-FIN] / [DEMO-FIN-2] (pas de calendrier glissant). */
    private static final LocalDate SCENARIO_DEMO_FIN_DATE_DEBUT = LocalDate.of(2026, 2, 28);
    private static final LocalDate SCENARIO_DEMO_FIN_DATE_FIN   = LocalDate.of(2026, 5, 31);

    // -----------------------------------------------------------------------
    // Repositories injectes
    // -----------------------------------------------------------------------
    private final PasswordEncoder                    passwordEncoder;
    private final FiliereRepository                  filiereRepository;
    private final EntrepriseRepository               entrepriseRepository;
    private final ResponsableServiceStagesRepository responsableServiceStagesRepository;
    private final EncadrantAcademiqueRepository      encadrantAcademiqueRepository;
    private final EncadrantProfessionnelRepository   encadrantProfessionnelRepository;
    private final ResponsableEntrepriseRepository    responsableEntrepriseRepository;
    private final StagiaireRepository                stagiaireRepository;
    private final StageRepository                    stageRepository;
    private final UtilisateurRepository              utilisateurRepository;
    private final ConventionStageRepository          conventionStageRepository;
    private final CahierStageRepository              cahierStageRepository;
    private final ReunionRepository                  reunionRepository;
    private final AbsenceRepository                  absenceRepository;
    private final PlatformTransactionManager       transactionManager;

    // -----------------------------------------------------------------------
    // Point d'entree
    // -----------------------------------------------------------------------

    /**
     * Orchestre la création idempotente de l'environnement de démonstration.
     *
     * <p>Insère filières, entreprises, utilisateurs (tous rôles) et cinq stages en état
     * initial « frais » ({@link StatutStage#PAS_COMMENCE}, sujet en attente).</p>
     *
     * @param args arguments Spring Boot (ignorés)
     */
    @Override
    public void run(String... args) {
        log.info("[DEMO-INIT] ========== Debut de l'initialisation des donnees de demonstration ==========");

        // Encode le mot de passe une seule fois (BCrypt est lent — ne pas boucler)
        final String pwd = passwordEncoder.encode(DEMO_PASSWORD);

        // === Etape 1 : Filieres =========================================
        Filiere gi  = creerFiliereIfAbsent("Genie Informatique",
                "Conception et developpement de systemes informatiques");
        Filiere rt  = creerFiliereIfAbsent("Reseaux et Telecommunications",
                "Administration et securite des reseaux");
        Filiere gl  = creerFiliereIfAbsent("Genie Logiciel",
                "Developpement logiciel et methodologies agiles");

        // === Etape 2 : Entreprises =======================================
        Entreprise telnet   = creerEntrepriseIfAbsent(
                "Telnet Tunisie",   "contact@telnet.tn",   "+21671100001",
                "2, Rue du Lac de Geneve, Tunis", "Technologies de l Information");
        Entreprise sofrecom = creerEntrepriseIfAbsent(
                "Sofrecom Tunisie", "contact@sofrecom.tn", "+21671100002",
                "Centre Urbain Nord, Tunis",      "Telecommunications");

        // === Etape 3 : Responsable du service des stages ================
        creerResponsableServiceStagesIfAbsent(
                "Ben Ali", "Karim",
                "karim.benali@fsegs.tn", "+21698100001", "RSS-001",
                pwd, "Service des Stages et Relations Entreprises");

        // === Etape 4 : Encadrants academiques ===========================
        EncadrantAcademique sonia = creerEncadrantAcademiqueIfAbsent(
                "Gharbi",   "Sonia",
                "sonia.gharbi@fsegs.tn",   "+21698100002", "EAC-001",
                pwd, "Maitre de Conferences", "Informatique et Systemes d Information");

        EncadrantAcademique hedi  = creerEncadrantAcademiqueIfAbsent(
                "Mansour",  "Hedi",
                "hedi.mansour@fsegs.tn",   "+21698100003", "EAC-002",
                pwd, "Maitre Assistant",   "Reseaux et Telecommunications");

        // === Etape 5 : Encadrants professionnels ========================
        EncadrantProfessionnel walid = creerEncadrantProfessionnelIfAbsent(
                "Trabelsi", "Walid",
                "walid.trabelsi@telnet.tn",   "+21698100004",
                pwd, "Chef de Projet", "Direction des Systemes d Information", telnet);

        EncadrantProfessionnel ines  = creerEncadrantProfessionnelIfAbsent(
                "Chaabane", "Ines",
                "ines.chaabane@sofrecom.tn",  "+21698100005",
                pwd, "Ingenieure Senior",     "Recherche et Developpement",        sofrecom);

        // === Etape 6 : Responsables entreprise (DRH / tuteur entreprise) ===
        ResponsableEntreprise tuteurTelnet = creerResponsableEntrepriseIfAbsent(
                "Jebali", "Mourad",
                "mourad.jebali@telnet.tn",  "+21698100006",
                pwd, "Directeur RH",    "Ressources Humaines", telnet);

        ResponsableEntreprise tuteurSofrecom = creerResponsableEntrepriseIfAbsent(
                "Saidi",  "Leila",
                "leila.saidi@sofrecom.tn",  "+21698100007",
                pwd, "Directrice RH",   "Ressources Humaines", sofrecom);

        // === Etape 7 : Stagiaires (un par stage) ========================
        Stagiaire ahmed   = creerStagiaireIfAbsent(
                "Ben Romdhane", "Ahmed",
                "ahmed.benromdhane@etudiant.tn",  "+21698200001", "STG-001",
                pwd, gi,  3, LocalDate.of(2002, 5, 14));

        Stagiaire mariam  = creerStagiaireIfAbsent(
                "Khelifi",  "Mariam",
                "mariam.khelifi@etudiant.tn",     "+21698200002", "STG-002",
                pwd, rt,  2, LocalDate.of(2003, 9, 22));

        Stagiaire youssef = creerStagiaireIfAbsent(
                "Hamdi",    "Youssef",
                "youssef.hamdi@etudiant.tn",      "+21698200003", "STG-003",
                pwd, gl,  3, LocalDate.of(2001, 3, 10));

        Stagiaire sara    = creerStagiaireIfAbsent(
                "Abidi",    "Sara",
                "sara.abidi@etudiant.tn",         "+21698200004", "STG-004",
                pwd, gi,  2, LocalDate.of(2003, 7, 18));

        Stagiaire mohamed = creerStagiaireIfAbsent(
                "Bouaziz",  "Mohamed",
                "mohamed.bouaziz@etudiant.tn",    "+21698200005", "STG-005",
                pwd, rt,  3, LocalDate.of(2002, 11, 5));

        // === Etape 8 : Stages (exactement 5) ============================
        // ┌────────────────────────────────────────────────────────────────┐
        // │ ETAT INITIAL FRAIS — REGLES METIER STRICTES :                  │
        // │  - tous les stages sont en PAS_COMMENCE (dates futures)        │
        // │  - sujet en EN_ATTENTE (NON valide par l'encadrant academique) │
        // │  - aucune signature, aucune evaluation, aucune convention      │
        // │    pre-generee : ces documents seront crees naturellement par  │
        // │    le workflow reel (affectation, validation, signature...).   │
        // │ Cela permet de tester chaque etape de bout en bout depuis un   │
        // │ etat realiste.                                                  │
        // └────────────────────────────────────────────────────────────────┘
        LocalDate baseStart = LocalDate.now().plusDays(30);    // demarre dans 30 jours

        // Stage 1 — A demarrer chez Telnet (3 mois)
        creerStageIfAbsent(
                "Developpement d'une application web de gestion RH",
                ahmed, sonia, walid, telnet, tuteurTelnet,
                baseStart,            baseStart.plusMonths(3),  12,
                "Conception et developpement d'un module de gestion des conges et absences "
                        + "integre dans le SI existant de l'entreprise.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 2 — A demarrer chez Sofrecom (3 mois)
        creerStageIfAbsent(
                "Mise en place d'une infrastructure reseau SDN",
                mariam, hedi, ines, sofrecom, tuteurSofrecom,
                baseStart.plusDays(15), baseStart.plusDays(15).plusMonths(3), 13,
                "Conception et deploiement d'un reseau defini par logiciel "
                        + "pour le datacenter principal de l'entreprise.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 3 — A demarrer chez Telnet (2 mois)
        creerStageIfAbsent(
                "Developpement d'une application mobile bancaire Flutter",
                youssef, sonia, walid, telnet, tuteurTelnet,
                baseStart.plusDays(7),  baseStart.plusDays(7).plusMonths(2),  8,
                "Application mobile cross-platform (iOS/Android) pour la consultation "
                        + "de comptes et les virements bancaires en temps reel.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 4 — A demarrer chez Telnet (3 mois)
        creerStageIfAbsent(
                "Systeme de gestion des stocks et approvisionnements",
                sara, sonia, walid, telnet, tuteurTelnet,
                baseStart.plusDays(20), baseStart.plusDays(20).plusMonths(3), 13,
                "Developpement d'un module de gestion des inventaires "
                        + "integre dans le SI existant de l'entreprise.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 5 — A demarrer chez Sofrecom (2 mois)
        creerStageIfAbsent(
                "Integration et securisation d'une API REST",
                mohamed, hedi, ines, sofrecom, tuteurSofrecom,
                baseStart.plusDays(10), baseStart.plusDays(10).plusMonths(2), 9,
                "Mise en place d'une API REST securisee avec authentification "
                        + "OAuth2 et journalisation des acces sensibles.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // === Etape 9 : Scenarios fin de stage [DEMO-FIN] (dates fixes 28/02 -> 31/05) ===
        initialiserScenariosFinDeStage(pwd, gl);

        assurerSignaturesProfilDemo();

        log.info("[DEMO-INIT] ========== Initialisation terminee — etat initial frais ==========");
    }

    /**
     * Les comptes de demonstration doivent pouvoir signer sans configuration manuelle du profil.
     */
    private void assurerSignaturesProfilDemo() {
        int patched = 0;
        for (Utilisateur utilisateur : utilisateurRepository.findAll()) {
            if (utilisateur.getEmail() == null || !isCompteDemonstration(utilisateur.getEmail())) {
                continue;
            }
            if (utilisateur.getUrlSignature() != null && !utilisateur.getUrlSignature().isBlank()) {
                continue;
            }
            utilisateur.setUrlSignature(DEMO_URL_SIGNATURE);
            utilisateurRepository.save(utilisateur);
            patched++;
        }
        if (patched > 0) {
            log.info("[DEMO-INIT] Signatures de profil de demonstration ajoutees pour {} utilisateur(s).", patched);
        }
    }

    private boolean isCompteDemonstration(String email) {
        String normalized = email.trim().toLowerCase();
        return normalized.endsWith("@etudiant.tn")
                || normalized.endsWith("@fsegs.tn")
                || normalized.endsWith("@telnet.tn")
                || normalized.endsWith("@sofrecom.tn")
                || normalized.endsWith("@techcorp.tn")
                || normalized.contains("demo.");
    }

    /**
     * Deux stages [DEMO-FIN] / [DEMO-FIN-2] : dates fixes 28/02/2026 → 31/05/2026.
     * Ne cree pas : FicheEvaluation, EnqueteSatisfaction, ReunionFinale, signatures.
     */
    private void initialiserScenariosFinDeStage(String encodedPassword, Filiere filiereGenieLogiciel) {
        log.info("[DEMO-INIT] --- Scenarios fin de stage (28/02/2026 -> 31/05/2026, 2 stages) ---");

        LocalDate dateDebut = SCENARIO_DEMO_FIN_DATE_DEBUT;
        LocalDate dateFin = SCENARIO_DEMO_FIN_DATE_FIN;

        Entreprise techCorp = creerEntrepriseIfAbsent(
                ENTREPRISE_DEMO_FIN_NOM,
                "contact@techcorp.tn",
                "+21671100099",
                "Immeuble Technopole, Lac 1, Tunis",
                "Developpement logiciel et services IT");

        EncadrantAcademique encadAcadFin = creerEncadrantAcademiqueIfAbsent(
                "Mezghani", "Nadia",
                "demo.fin.acad@fsegs.tn", "+21698100091", "EAC-DEMO-FIN",
                encodedPassword, "Maitre Assistant", "Genie Logiciel");

        EncadrantProfessionnel encadProFin = creerEncadrantProfessionnelIfAbsent(
                "Bouslama", "Karim",
                "demo.fin.pro@techcorp.tn", "+21698100092",
                encodedPassword, "Lead Developer", "R&D Produit", techCorp);

        ResponsableEntreprise tuteurTechCorp = creerResponsableEntrepriseIfAbsent(
                "Gharbi", "Salma",
                "demo.fin.resp@techcorp.tn", "+21698100093",
                encodedPassword, "Responsable RH", "Ressources Humaines", techCorp);

        if (!validerEncadrantsEtEntrepriseScenarioFin(encadAcadFin, encadProFin, tuteurTechCorp, techCorp)) {
            log.warn("[DEMO-INIT] Scenarios fin de stage abandonnes : encadrants ou entreprise invalides.");
            return;
        }

        Stagiaire stagiaireFinA = creerStagiaireIfAbsent(
                "Ferchichi", "Amine",
                "demo.fin.stagiaire@etudiant.tn", "+21698200091", "STG-DEMO-FIN",
                encodedPassword, filiereGenieLogiciel, 3, LocalDate.of(2002, 1, 20));

        Stagiaire stagiaireFinB = creerStagiaireIfAbsent(
                "Mansouri", "Leila",
                "demo.fin2.stagiaire@etudiant.tn", "+21698200092", "STG-DEMO-FIN-2",
                encodedPassword, filiereGenieLogiciel, 3, LocalDate.of(2002, 6, 12));

        peuplerScenarioFinDeStage(
                SCENARIO_FIN_STAGE_TITRE,
                stagiaireFinA,
                encadAcadFin,
                encadProFin,
                tuteurTechCorp,
                techCorp,
                dateDebut,
                dateFin);

        peuplerScenarioFinDeStage(
                SCENARIO_FIN_STAGE_2_TITRE,
                stagiaireFinB,
                encadAcadFin,
                encadProFin,
                tuteurTechCorp,
                techCorp,
                dateDebut,
                dateFin);

        log.info("[DEMO-INIT] Comptes partages scenario fin (mot de passe : {}) :", DEMO_PASSWORD);
        log.info("[DEMO-INIT]   Encadrant academique    : demo.fin.acad@fsegs.tn");
        log.info("[DEMO-INIT]   Encadrant professionnel : demo.fin.pro@techcorp.tn");
        log.info("[DEMO-INIT]   Tuteur entreprise (RH)   : demo.fin.resp@techcorp.tn");
        log.info("[DEMO-INIT]   Stagiaire stage A        : demo.fin.stagiaire@etudiant.tn");
        log.info("[DEMO-INIT]   Stagiaire stage B        : demo.fin2.stagiaire@etudiant.tn");
    }

    /**
     * Cree ou met a jour un stage de demonstration « fin de stage » et ses artefacts lies.
     */
    private void peuplerScenarioFinDeStage(
            String titreStage,
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise,
            Entreprise entreprise,
            LocalDate dateDebut,
            LocalDate dateFin) {

        if (!validerActeursScenarioFin(stagiaire, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise, entreprise)) {
            log.warn("[DEMO-INIT] Scenario fin ignore pour « {} » : acteur manquant.", titreStage);
            return;
        }

        Stage stage = creerOuRecupererStageFinDemo(
                titreStage,
                stagiaire,
                encadrantAcademique,
                encadrantProfessionnel,
                tuteurEntreprise,
                entreprise,
                dateDebut,
                dateFin
        );
        if (stage == null) {
            return;
        }

        peuplerArtefactsScenarioFinDeStage(
                stage, stagiaire, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise);

        log.info("[DEMO-INIT] Scenario fin pret — stageId={} | titre={} | stagiaire={} | dateFin={}",
                stage.getId(),
                titreStage,
                stagiaire.getEmail(),
                dateFin);
    }

    /**
     * Convention, cahier, 5 reunions avec observations, 2 absences (1 justifiee, 1 non) — sans signatures.
     */
    private void peuplerArtefactsScenarioFinDeStage(
            Stage stage,
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise) {

        Stage managed = stageRepository.findById(stage.getId()).orElse(stage);
        creerConventionStageFinDemoIfAbsent(managed);
        CahierStage cahier = creerCahierStageFinDemoIfAbsent(managed);
        creerReunionsHebdomadairesFinDemoIfAbsent(
                managed, cahier, stagiaire, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise);
        creerAbsencesFinDemoIfAbsent(managed);
    }

    private boolean validerEncadrantsEtEntrepriseScenarioFin(
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise,
            Entreprise entreprise) {
        return validerActeursScenarioFin(null, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise, entreprise, false);
    }

    private boolean validerActeursScenarioFin(
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise,
            Entreprise entreprise) {
        return validerActeursScenarioFin(
                stagiaire, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise, entreprise, true);
    }

    private boolean validerActeursScenarioFin(
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise,
            Entreprise entreprise,
            boolean exigerStagiaire) {
        if (exigerStagiaire && stagiaire == null) {
            log.warn("[DEMO-INIT] Validation scenario fin : stagiaire null.");
            return false;
        }
        if (encadrantAcademique == null) {
            log.warn("[DEMO-INIT] Validation scenario fin : encadrant academique null.");
            return false;
        }
        if (encadrantProfessionnel == null) {
            log.warn("[DEMO-INIT] Validation scenario fin : encadrant professionnel null.");
            return false;
        }
        if (tuteurEntreprise == null) {
            log.warn("[DEMO-INIT] Validation scenario fin : tuteur entreprise null.");
            return false;
        }
        if (entreprise == null) {
            log.warn("[DEMO-INIT] Validation scenario fin : entreprise null.");
            return false;
        }
        if (encadrantProfessionnel.getEntreprise() == null
                || !entreprise.getId().equals(encadrantProfessionnel.getEntreprise().getId())) {
            log.warn("[DEMO-INIT] Validation scenario fin : encadrant pro non rattache a {}.", entreprise.getNom());
            return false;
        }
        if (tuteurEntreprise.getEntreprise() == null
                || !entreprise.getId().equals(tuteurEntreprise.getEntreprise().getId())) {
            log.warn("[DEMO-INIT] Validation scenario fin : tuteur entreprise non rattache a {}.", entreprise.getNom());
            return false;
        }
        return true;
    }

    private Stage creerOuRecupererStageFinDemo(
            String titreStage,
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise,
            Entreprise entreprise,
            LocalDate dateDebut,
            LocalDate dateFin) {

        List<Stage> existants = stageRepository.findByStagiaireIdAndTitreIn(
                stagiaire.getId(), List.of(titreStage));
        if (!existants.isEmpty()) {
            Stage existing = existants.get(0);
            synchroniserDatesStageFinDemo(existing.getId(), dateDebut, dateFin, tuteurEntreprise);
            log.debug("[DEMO-INIT] Stage scenario fin deja present (id={}, titre={}).", existing.getId(), titreStage);
            return stageRepository.findById(existing.getId()).orElse(existing);
        }

        if (!validerActeursScenarioFin(stagiaire, encadrantAcademique, encadrantProfessionnel, tuteurEntreprise, entreprise)) {
            return null;
        }

        String sujet = "Conception et developpement d'une plateforme web de suivi de stages "
                + "(conventions, cahiers, evaluations) pour le service des stages de la FSEGS.";

        Stage stage = new Stage();
        stage.setTitre(titreStage);
        stage.setSujet(sujet);
        stage.setStatut(StatutStage.EN_COURS);
        stage.setStatutSujet(StatutValidation.VALIDEE);
        stage.setStagiaire(stagiaire);
        stage.setEncadrantAcademique(encadrantAcademique);
        stage.setEncadrantProfessionnel(encadrantProfessionnel);
        stage.setTuteurEntreprise(
                responsableEntrepriseRepository.getReferenceById(tuteurEntreprise.getId()));
        stage.setEntreprise(entreprise);
        stage.setDateDebut(dateDebut);
        stage.setDateFin(dateFin);
        stage.setNbSemaine(12);
        stage.setDuree(3);
        stage.setSectionEvaluationOuverte(false);
        stage.setNotificationOuvertureEspacesEnvoyee(false);

        Stage saved = stageRepository.save(stage);
        log.info("[DEMO-INIT] Stage scenario fin cree : id={} | titre={} | {} -> {} | tuteur={} | statut={}",
                saved.getId(), titreStage, dateDebut, dateFin,
                tuteurEntreprise.getPrenom() + " " + tuteurEntreprise.getNom(),
                saved.getStatut());
        return saved;
    }

    private void synchroniserDatesStageFinDemo(
            Long stageId,
            LocalDate dateDebut,
            LocalDate dateFin,
            ResponsableEntreprise tuteurEntreprise) {
        Stage managed = stageRepository.findById(stageId).orElse(null);
        if (managed == null) {
            return;
        }

        boolean changed = false;
        if (managed.getDateDebut() == null || !dateDebut.equals(managed.getDateDebut())) {
            managed.setDateDebut(dateDebut);
            changed = true;
        }
        if (managed.getDateFin() == null || !dateFin.equals(managed.getDateFin())) {
            managed.setDateFin(dateFin);
            changed = true;
        }
        if (managed.getStatut() != StatutStage.EN_COURS) {
            managed.setStatut(StatutStage.EN_COURS);
            changed = true;
        }
        if (managed.getStatutSujet() != StatutValidation.VALIDEE) {
            managed.setStatutSujet(StatutValidation.VALIDEE);
            changed = true;
        }
        Long tuteurId = tuteurEntreprise != null ? tuteurEntreprise.getId() : null;
        Long currentTuteurId = managed.getTuteurEntreprise() != null ? managed.getTuteurEntreprise().getId() : null;
        if (tuteurId != null && !tuteurId.equals(currentTuteurId)) {
            managed.setTuteurEntreprise(responsableEntrepriseRepository.getReferenceById(tuteurId));
            changed = true;
        }
        if (changed) {
            stageRepository.save(managed);
            log.info("[DEMO-INIT] Stage scenario fin id={} — dates/statut/tuteur realignes.", stageId);
        }
    }

    private void creerConventionStageFinDemoIfAbsent(Stage stage) {
        Long stageId = stage.getId();
        if (conventionStageRepository.existsByStageId(stageId)) {
            return;
        }
        Stage stageRef = stageRepository.getReferenceById(stageId);
        ConventionStage convention = new ConventionStage();
        convention.setStage(stageRef);
        convention.setDateDebut(stage.getDateDebut());
        convention.setDateFin(stage.getDateFin());
        convention.setNumConv(stageId.intValue());
        convention.setSignatures(new ArrayList<>());
        conventionStageRepository.save(convention);
        log.info("[DEMO-INIT] Convention scenario fin creee pour stage id={} (sans signatures).", stageId);
    }

    private CahierStage creerCahierStageFinDemoIfAbsent(Stage stage) {
        Long stageId = stage.getId();
        return cahierStageRepository.findByStageId(stageId).orElseGet(() -> {
            CahierStage cahier = new CahierStage();
            cahier.setStage(stageRepository.getReferenceById(stageId));
            cahier.setDateGeneration(stage.getDateDebut());
            cahier.setSignatures(new ArrayList<>());
            CahierStage saved = cahierStageRepository.save(cahier);
            log.info("[DEMO-INIT] Cahier scenario fin cree pour stage id={} (sans signatures).", stageId);
            return saved;
        });
    }

    private void creerReunionsHebdomadairesFinDemoIfAbsent(
            Stage stage,
            CahierStage cahier,
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            ResponsableEntreprise tuteurEntreprise) {

        Long stagiaireId = stagiaire.getId();
        Long encadAcadId = encadrantAcademique.getId();
        Long encadProId = encadrantProfessionnel.getId();
        Long tuteurId = tuteurEntreprise != null ? tuteurEntreprise.getId() : null;

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                creerReunionsHebdomadairesFinDemoDansTransaction(
                        stage, cahier, stagiaireId, encadAcadId, encadProId, tuteurId,
                        encadrantAcademique, encadrantProfessionnel));
    }

    private void creerReunionsHebdomadairesFinDemoDansTransaction(
            Stage stage,
            CahierStage cahier,
            Long stagiaireId,
            Long encadrantAcademiqueId,
            Long encadrantProfessionnelId,
            Long tuteurEntrepriseId,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel) {

        record ReunionPlan(
                int semainesApresDebut,
                LocalTime heure,
                String createurType,
                Utilisateur createur,
                String observation) {}

        List<ReunionPlan> plans = List.of(
                new ReunionPlan(2, LocalTime.of(10, 0), "ENCADRANT_ACADEMIQUE", encadrantAcademique,
                        "Lancement : cadrage du besoin, maquettes Figma validees, backlog Trello initialise."),
                new ReunionPlan(4, LocalTime.of(14, 30), "ENCADRANT_PROFESSIONNEL", encadrantProfessionnel,
                        "Sprint 1 : API REST conventions operationnelle, revue de code avec l'equipe R&D."),
                new ReunionPlan(6, LocalTime.of(11, 0), "ENCADRANT_ACADEMIQUE", encadrantAcademique,
                        "Point mi-parcours : module signatures en cours, tests unitaires a renforcer."),
                new ReunionPlan(8, LocalTime.of(15, 0), "ENCADRANT_PROFESSIONNEL", encadrantProfessionnel,
                        "Sprint 2 : generation PDF cahier/fiche, retours UX du responsable RH integres."),
                new ReunionPlan(11, LocalTime.of(9, 30), "ENCADRANT_ACADEMIQUE", encadrantAcademique,
                        "Preparation cloture : recapitulatif des livrables, plan de signature et soutenance.")
        );

        LocalDate debut = stage.getDateDebut();
        int index = 1;
        for (ReunionPlan plan : plans) {
            LocalDate date = debut.plusWeeks(plan.semainesApresDebut());
            if (reunionRepository.existsByStageIdAndDateAndHeure(stage.getId(), date, plan.heure())) {
                index++;
                continue;
            }

            ReunionHebdomadaire reunion = new ReunionHebdomadaire();
            reunion.setStage(stageRepository.getReferenceById(stage.getId()));
            reunion.setCahierStage(cahier);
            reunion.setNumReunion(String.format("REU-DEMO-FIN-%d-%02d", stage.getId(), index));
            reunion.setDate(date);
            reunion.setHeure(plan.heure());
            reunion.setEncadrantCreateurId(plan.createur().getId());
            reunion.setTypeEncadrantCreateur(plan.createurType());
            reunion.setNomEncadrantCreateur(plan.createur().getPrenom() + " " + plan.createur().getNom());
            if ("ENCADRANT_ACADEMIQUE".equals(plan.createurType())) {
                reunion.setObservationEncadrantAcademique(plan.observation());
            } else {
                reunion.setObservationEncadrantProfessionnel(plan.observation());
            }
            reunion.setCompteRendu(plan.observation());

            reunion.setParticipants(participantsReunionFinDemo(
                    stagiaireId, encadrantAcademiqueId, encadrantProfessionnelId, tuteurEntrepriseId));

            reunionRepository.save(reunion);
            index++;
        }
        log.info("[DEMO-INIT] Reunions hebdomadaires scenario fin assurees pour stage id={}.", stage.getId());
    }

    /**
     * Charge les participants dans la transaction courante (requis avant {@link Set#add} / hashCode).
     */
    private Set<Utilisateur> participantsReunionFinDemo(
            Long stagiaireId,
            Long encadrantAcademiqueId,
            Long encadrantProfessionnelId,
            Long tuteurEntrepriseId) {
        Set<Utilisateur> participants = new LinkedHashSet<>();
        ajouterParticipantReunion(participants, stagiaireId);
        ajouterParticipantReunion(participants, encadrantAcademiqueId);
        ajouterParticipantReunion(participants, encadrantProfessionnelId);
        ajouterParticipantReunion(participants, tuteurEntrepriseId);
        return participants;
    }

    private void ajouterParticipantReunion(Set<Utilisateur> participants, Long utilisateurId) {
        if (utilisateurId == null) {
            return;
        }
        utilisateurRepository.findById(utilisateurId).ifPresent(participants::add);
    }

    private void creerAbsencesFinDemoIfAbsent(Stage stage) {
        LocalDate debut = stage.getDateDebut();
        Long stageId = stage.getId();

        if (!absenceAvecStatutExiste(stageId, "JUSTIFIEE")) {
            Absence justifiee = new Absence();
            justifiee.setStage(stageRepository.getReferenceById(stage.getId()));
            justifiee.setDateAbsence(debut.plusWeeks(3));
            justifiee.setNbAbsence(1);
            justifiee.setStatut("JUSTIFIEE");
            justifiee.setJustification("Certificat medical — grippe (1 jour).");
            justifiee.setCommentaire("Absence signalee et validee par le responsable RH.");
            absenceRepository.save(justifiee);
        }

        if (!absenceAvecStatutExiste(stageId, "NON_JUSTIFIEE")) {
            Absence nonJustifiee = new Absence();
            nonJustifiee.setStage(stageRepository.getReferenceById(stage.getId()));
            nonJustifiee.setDateAbsence(debut.plusWeeks(7));
            nonJustifiee.setNbAbsence(1);
            nonJustifiee.setStatut("NON_JUSTIFIEE");
            nonJustifiee.setJustification("Absence non justifiee dans les delais.");
            nonJustifiee.setCommentaire("Retard de justification — a discuter en reunion de cloture.");
            absenceRepository.save(nonJustifiee);
        }

        log.info("[DEMO-INIT] Absences scenario fin assurees pour stage id={}.", stageId);
    }

    private boolean absenceAvecStatutExiste(Long stageId, String statut) {
        return absenceRepository.findByStageId(stageId).stream()
                .anyMatch(a -> statut.equalsIgnoreCase(a.getStatut()));
    }

    // =========================================================================
    // SECTION 1 — FILIERES
    // =========================================================================

    /**
     * Cree une filiere si aucune n'existe avec ce nom (insensible a la casse).
     *
     * @return l'entite existante ou nouvellement creee
     */
    private Filiere creerFiliereIfAbsent(String nom, String description) {
        return filiereRepository.findByNomIgnoreCase(nom).orElseGet(() -> {
            Filiere f = new Filiere();
            f.setNom(nom);
            f.setDescription(description);
            Filiere saved = filiereRepository.save(f);
            log.info("[DEMO-INIT] Filiere creee : {}", nom);
            return saved;
        });
    }

    // =========================================================================
    // SECTION 2 — ENTREPRISES
    // =========================================================================

    /**
     * Cree une entreprise si aucune n'existe avec ce nom (insensible a la casse).
     *
     * @return l'entite existante ou nouvellement creee
     */
    private Entreprise creerEntrepriseIfAbsent(
            String nom, String email, String tel, String adresse, String secteur) {
        return entrepriseRepository.findByNomIgnoreCase(nom).orElseGet(() -> {
            Entreprise e = Entreprise.builder()
                    .nom(nom)
                    .email(email)
                    .telephone(tel)
                    .adresse(adresse)
                    .secteurActivite(secteur)
                    .build();
            Entreprise saved = entrepriseRepository.save(e);
            log.info("[DEMO-INIT] Entreprise creee : {} ({})", nom, secteur);
            return saved;
        });
    }

    // =========================================================================
    // SECTION 3 — RESPONSABLE SERVICE STAGES
    // =========================================================================

    /**
     * Cree le responsable du service des stages si son email n'est pas encore present.
     *
     * <p>Cette entite n'a pas de @SuperBuilder — on utilise les setters heri tes de Utilisateur.
     * Les champs @Builder.Default (actif, supprime, doitChangerMotDePasse) doivent etre
     * positionnes explicitement car le constructeur no-args ne les initialise pas.
     */
    private void creerResponsableServiceStagesIfAbsent(
            String nom, String prenom, String email, String tel, String matricule,
            String encodedPassword, String service) {
        if (responsableServiceStagesRepository.existsByEmail(email)) {
            log.debug("[DEMO-INIT] ResponsableServiceStages deja present : {}", email);
            return;
        }
        // Verifier aussi l'unicite globale de l'email dans la table utilisateur
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            log.warn("[DEMO-INIT] Email {} deja utilise par un autre utilisateur.", email);
            return;
        }

        ResponsableServiceStages rss = new ResponsableServiceStages();
        rss.setNom(nom);
        rss.setPrenom(prenom);
        rss.setEmail(email);
        rss.setTelephone(tel);
        rss.setMatricule(matricule);
        rss.setMotDePasse(encodedPassword);
        rss.setRole(Role.RESPONSABLE_STAGE);
        // @Builder.Default ne s'applique pas au constructeur — forcer les valeurs
        rss.setActif(true);
        rss.setSupprime(false);
        rss.setDoitChangerMotDePasse(false);
        rss.setService(service);

        responsableServiceStagesRepository.save(rss);
        log.info("[DEMO-INIT] ResponsableServiceStages cree : {} {} <{}>", prenom, nom, email);
    }

    // =========================================================================
    // SECTION 4 — ENCADRANTS ACADEMIQUES
    // =========================================================================

    /**
     * Cree un encadrant academique si son matricule n'est pas encore present.
     * Controle secondaire sur l'email pour respecter la contrainte UNIQUE.
     *
     * @return l'entite existante ou nouvellement creee (null si conflit email)
     */
    private EncadrantAcademique creerEncadrantAcademiqueIfAbsent(
            String nom, String prenom, String email, String tel, String matricule,
            String encodedPassword, String grade, String specialite) {
        return encadrantAcademiqueRepository.findByMatricule(matricule).orElseGet(() -> {
            if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
                log.warn("[DEMO-INIT] Email {} deja utilise, EncadrantAcademique non cree.", email);
                return null;
            }
            EncadrantAcademique ea = EncadrantAcademique.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(tel)
                    .matricule(matricule)
                    .motDePasse(encodedPassword)
                    .role(Role.ENCADRANT_ACADEMIQUE)
                    .actif(true)
                    .supprime(false)
                    .doitChangerMotDePasse(false)
                    .grade(grade)
                    .specialite(specialite)
                    .build();
            EncadrantAcademique saved = encadrantAcademiqueRepository.save(ea);
            log.info("[DEMO-INIT] EncadrantAcademique cree : {} {} — {} en {}",
                    prenom, nom, grade, specialite);
            return saved;
        });
    }

    // =========================================================================
    // SECTION 5 — ENCADRANTS PROFESSIONNELS
    // =========================================================================

    /**
     * Cree un encadrant professionnel si son email n'est pas encore present.
     * Le lien vers l'entreprise est etabli directement (FK encadrant_professionnel.entreprise_id).
     *
     * <p>Controle secondaire sur l'email pour respecter la contrainte UNIQUE heritee
     * de la table {@code utilisateur} : si l'email est deja utilise par un utilisateur
     * d'un autre role, la creation est annulee et {@code null} est retourne.</p>
     *
     * @return l'entite existante ou nouvellement creee, ou {@code null} si conflit email
     */
    private EncadrantProfessionnel creerEncadrantProfessionnelIfAbsent(
            String nom, String prenom, String email, String tel,
            String encodedPassword, String poste, String service, Entreprise entreprise) {
        return encadrantProfessionnelRepository.findByEmail(email).orElseGet(() -> {
            if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
                log.warn("[DEMO-INIT] Email {} deja utilise, EncadrantProfessionnel non cree.", email);
                return null;
            }
            EncadrantProfessionnel ep = EncadrantProfessionnel.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(tel)
                    .motDePasse(encodedPassword)
                    .role(Role.ENCADRANT_PROFESSIONNEL)
                    .actif(true)
                    .supprime(false)
                    .doitChangerMotDePasse(false)
                    .poste(poste)
                    .service(service)
                    .entreprise(entreprise)
                    .build();
            EncadrantProfessionnel saved = encadrantProfessionnelRepository.save(ep);
            log.info("[DEMO-INIT] EncadrantProfessionnel cree : {} {} — {} chez {}",
                    prenom, nom, poste, entreprise.getNom());
            return saved;
        });
    }

    // =========================================================================
    // SECTION 6 — RESPONSABLES ENTREPRISE
    // =========================================================================

    /**
     * Cree un responsable entreprise (DRH / tuteur entreprise) si son email n'est pas encore present.
     * Utilise le builder @SuperBuilder herite de Utilisateur.
     *
     * @return l'entite existante ou nouvellement creee, ou {@code null} si conflit email
     */
    private ResponsableEntreprise creerResponsableEntrepriseIfAbsent(
            String nom, String prenom, String email, String tel,
            String encodedPassword, String poste, String service, Entreprise entreprise) {
        return responsableEntrepriseRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
                log.warn("[DEMO-INIT] Email {} deja utilise par un autre utilisateur.", email);
                return null;
            }

            ResponsableEntreprise re = ResponsableEntreprise.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(tel)
                    .motDePasse(encodedPassword)
                    .role(Role.RESPONSABLE_ENTREPRISE)
                    .actif(true)
                    .supprime(false)
                    .doitChangerMotDePasse(false)
                    .poste(poste)
                    .service(service)
                    .entreprise(entreprise)
                    .build();
            ResponsableEntreprise saved = responsableEntrepriseRepository.save(re);
            log.info("[DEMO-INIT] ResponsableEntreprise cree : {} {} — {} chez {}",
                    prenom, nom, poste, entreprise.getNom());
            return saved;
        });
    }

    // =========================================================================
    // SECTION 7 — STAGIAIRES
    // =========================================================================

    /**
     * Cree un stagiaire si son matricule n'est pas encore present.
     * Controle secondaire sur l'email (contrainte UNIQUE dans utilisateur).
     *
     * @return l'entite existante ou nouvellement creee (null si conflit)
     */
    private Stagiaire creerStagiaireIfAbsent(
            String nom, String prenom, String email, String tel, String matricule,
            String encodedPassword, Filiere filiere, int niveau, LocalDate dateNaiss) {
        return stagiaireRepository.findByMatricule(matricule).orElseGet(() -> {
            if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
                log.warn("[DEMO-INIT] Email {} deja utilise, Stagiaire non cree.", email);
                return null;
            }
            Stagiaire s = Stagiaire.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(tel)
                    .matricule(matricule)
                    .motDePasse(encodedPassword)
                    .role(Role.STAGIAIRE)
                    .actif(true)
                    .supprime(false)
                    .doitChangerMotDePasse(false)
                    .filiere(filiere)
                    .niveau(niveau)
                    .dateNaiss(dateNaiss)
                    .build();
            Stagiaire saved = stagiaireRepository.save(s);
            log.info("[DEMO-INIT] Stagiaire cree : {} {} (Filiere: {}, Niveau: {})",
                    prenom, nom, filiere.getNom(), niveau);
            return saved;
        });
    }

    // =========================================================================
    // SECTION 8 — STAGES (exactement 5)
    // =========================================================================

    /**
     * Cree un stage si le stagiaire n'a pas encore de stage portant ce titre.
     *
     * <p>Controle d'idempotence : on cherche un stage existant pour le couple
     * (stagiaireId, titre). Si trouve, on saute la creation.
     *
     * <p>Contrainte respectee : {@code Stage.entreprise} est {@code nullable = false}
     * — on exige toujours une entreprise non nulle.
     *
     * @param encadrantProfessionnel obligatoire — ne jamais passer null (violation metier)
     * @param tuteurEntreprise       responsable entreprise designe tuteur du stage (obligatoire metier)
     */
    private void creerStageIfAbsent(
            String titre,
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            Entreprise entreprise,
            ResponsableEntreprise tuteurEntreprise,
            LocalDate dateDebut,
            LocalDate dateFin,
            int nbSemaine,
            String sujet,
            StatutStage statut,
            StatutValidation statutSujet) {

        // Garde-fous : ne pas inserer un stage avec des FK invalides
        if (stagiaire == null) {
            log.warn("[DEMO-INIT] Stage '{}' ignore : stagiaire est null.", titre);
            return;
        }
        if (entreprise == null) {
            log.warn("[DEMO-INIT] Stage '{}' ignore : entreprise est null (NOT NULL constraint).", titre);
            return;
        }
        if (encadrantProfessionnel == null) {
            log.warn("[DEMO-INIT] Stage '{}' ignore : encadrantProfessionnel est null — violation metier.", titre);
            return;
        }
        if (encadrantAcademique == null) {
            log.warn("[DEMO-INIT] Stage '{}' ignore : encadrantAcademique est null — violation metier.", titre);
            return;
        }
        if (tuteurEntreprise == null) {
            log.warn("[DEMO-INIT] Stage '{}' ignore : tuteurEntreprise est null — violation metier.", titre);
            return;
        }

        // Controle idempotence : un stage avec ce titre existe-t-il pour ce stagiaire ?
        List<Stage> existants = stageRepository.findByStagiaireIdAndTitreIn(stagiaire.getId(), List.of(titre));
        if (!existants.isEmpty()) {
            Stage existing = existants.get(0);
            Long currentTuteurId = existing.getTuteurEntreprise() != null
                    ? existing.getTuteurEntreprise().getId() : null;
            if (tuteurEntreprise != null && !tuteurEntreprise.getId().equals(currentTuteurId)) {
                Stage managed = stageRepository.findById(existing.getId()).orElse(existing);
                managed.setTuteurEntreprise(
                        responsableEntrepriseRepository.getReferenceById(tuteurEntreprise.getId()));
                stageRepository.save(managed);
                log.info("[DEMO-INIT] Stage '{}' — tuteur entreprise renseigne (id={}).", titre, existing.getId());
            } else {
                log.debug("[DEMO-INIT] Stage '{}' deja present pour {}.", titre, stagiaire.getEmail());
            }
            return;
        }

        Stage stage = new Stage();
        stage.setTitre(titre);
        stage.setSujet(sujet);
        stage.setStatut(statut);
        stage.setStatutSujet(statutSujet);
        stage.setStagiaire(stagiaire);
        stage.setEncadrantAcademique(encadrantAcademique);   // obligatoire — garanti non null par les gardes ci-dessus
        stage.setEncadrantProfessionnel(encadrantProfessionnel); // obligatoire — garanti non null par les gardes ci-dessus
        stage.setTuteurEntreprise(
                responsableEntrepriseRepository.getReferenceById(tuteurEntreprise.getId()));
        stage.setEntreprise(entreprise);                     // NOT NULL — toujours fourni
        stage.setDateDebut(dateDebut);
        stage.setDateFin(dateFin);
        stage.setNbSemaine(nbSemaine);
        // Duree en mois : 4 semaines = 1 mois, plafonne entre 1 et 4 mois (regle metier)
        stage.setDuree(Math.max(1, Math.min(4, nbSemaine / 4)));
        // Sections ferm ees par defaut — ouvertes par le responsable en cours de stage
        stage.setSectionEvaluationOuverte(false);
        stage.setNotificationOuvertureEspacesEnvoyee(false);

        stageRepository.save(stage);
        log.info("[DEMO-INIT] Stage cree : '{}' | Stagiaire: {} {} | Entreprise: {} | Statut: {}",
                titre,
                stagiaire.getPrenom(), stagiaire.getNom(),
                entreprise.getNom(),
                statut);
    }
}
