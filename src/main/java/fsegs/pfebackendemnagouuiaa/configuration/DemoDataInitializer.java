package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.Filiere;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FiliereRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableServiceStagesRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Initialise la base de donnees avec des donnees de demonstration coherentes.
 *
 * <p>IDEMPOTENT : verifie l'existence avant chaque insertion — aucune duplication
 * au redemarrage.
 *
 * <p>Ordre d'execution :
 * <ol>
 *   <li>{@code @Order(0)}  — SchemaCompatibilityRunner (corrections de schema)</li>
 *   <li>{@code @Order(50)} — DemoDataInitializer      (ce runner)</li>
 *   <li>{@code @Order(100)} — EnqueteSatisfactionInitializationRunner (traite les stages crees ici)</li>
 * </ol>
 *
 * <p>Aucun script SQL n'est utilise. Mot de passe encode via BCrypt.
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

    // -----------------------------------------------------------------------
    // Point d'entree
    // -----------------------------------------------------------------------
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
        Entreprise biat     = creerEntrepriseIfAbsent(
                "BIAT Digital",    "contact@biat.com.tn", "+21671100003",
                "70-72 Avenue Habib Bourguiba, Tunis", "Finance et Banque");

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

        // === Etape 6 : Responsables entreprise (DRH) ====================
        creerResponsableEntrepriseIfAbsent(
                "Jebali", "Mourad",
                "mourad.jebali@telnet.tn",  "+21698100006",
                pwd, "Directeur RH",    "Ressources Humaines", telnet);

        creerResponsableEntrepriseIfAbsent(
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
                ahmed, sonia, walid, telnet,
                baseStart,            baseStart.plusMonths(3),  12,
                "Conception et developpement d'un module de gestion des conges et absences "
                        + "integre dans le SI existant de l'entreprise.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 2 — A demarrer chez Sofrecom (3 mois)
        creerStageIfAbsent(
                "Mise en place d'une infrastructure reseau SDN",
                mariam, hedi, ines, sofrecom,
                baseStart.plusDays(15), baseStart.plusDays(15).plusMonths(3), 13,
                "Conception et deploiement d'un reseau defini par logiciel "
                        + "pour le datacenter principal de l'entreprise.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 3 — A demarrer chez Telnet (2 mois)
        creerStageIfAbsent(
                "Developpement d'une application mobile bancaire Flutter",
                youssef, sonia, walid, telnet,
                baseStart.plusDays(7),  baseStart.plusDays(7).plusMonths(2),  8,
                "Application mobile cross-platform (iOS/Android) pour la consultation "
                        + "de comptes et les virements bancaires en temps reel.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 4 — A demarrer chez BIAT (3 mois)
        creerStageIfAbsent(
                "Systeme de gestion des stocks et approvisionnements",
                sara, hedi, null, biat,
                baseStart.plusDays(20), baseStart.plusDays(20).plusMonths(3), 13,
                "Developpement d'un ERP leger pour la gestion des inventaires "
                        + "et l'optimisation de la chaine d'approvisionnement.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        // Stage 5 — A demarrer chez BIAT (2 mois)
        creerStageIfAbsent(
                "Integration et securisation d'une API bancaire REST",
                mohamed, sonia, null, biat,
                baseStart.plusDays(10), baseStart.plusDays(10).plusMonths(2), 9,
                "Mise en place d'une API REST securisee avec authentification "
                        + "OAuth2 et journalisation des acces sensibles.",
                StatutStage.PAS_COMMENCE, StatutValidation.EN_ATTENTE);

        log.info("[DEMO-INIT] ========== Initialisation terminee — etat initial frais ==========");
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
     * Cree un responsable entreprise (DRH) si son email n'est pas encore present.
     * Utilise le builder @SuperBuilder herite de Utilisateur.
     */
    private void creerResponsableEntrepriseIfAbsent(
            String nom, String prenom, String email, String tel,
            String encodedPassword, String poste, String service, Entreprise entreprise) {
        if (responsableEntrepriseRepository.existsByEmail(email)) {
            log.debug("[DEMO-INIT] ResponsableEntreprise deja present : {}", email);
            return;
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            log.warn("[DEMO-INIT] Email {} deja utilise par un autre utilisateur.", email);
            return;
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
        responsableEntrepriseRepository.save(re);
        log.info("[DEMO-INIT] ResponsableEntreprise cree : {} {} — {} chez {}",
                prenom, nom, poste, entreprise.getNom());
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
     * @param encadrantProfessionnel peut etre null (FK nullable)
     */
    private void creerStageIfAbsent(
            String titre,
            Stagiaire stagiaire,
            EncadrantAcademique encadrantAcademique,
            EncadrantProfessionnel encadrantProfessionnel,
            Entreprise entreprise,
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

        // Controle idempotence : un stage avec ce titre existe-t-il pour ce stagiaire ?
        boolean dejaExistant = !stageRepository
                .findByStagiaireIdAndTitreIn(stagiaire.getId(), List.of(titre))
                .isEmpty();
        if (dejaExistant) {
            log.debug("[DEMO-INIT] Stage '{}' deja present pour {}.", titre, stagiaire.getEmail());
            return;
        }

        Stage stage = new Stage();
        stage.setTitre(titre);
        stage.setSujet(sujet);
        stage.setStatut(statut);
        stage.setStatutSujet(statutSujet);
        stage.setStagiaire(stagiaire);
        stage.setEncadrantAcademique(encadrantAcademique);   // nullable — ok si null
        stage.setEncadrantProfessionnel(encadrantProfessionnel); // nullable — ok si null
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
