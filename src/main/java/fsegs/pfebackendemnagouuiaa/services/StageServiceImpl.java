package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.entities.*;
import fsegs.pfebackendemnagouuiaa.mapper.StageMapper;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.OffreStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EncadrantAcademiqueRepository encadrantAcademiqueRepository;
    private final EncadrantProfessionnelRepository encadrantProfessionnelRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final OffreStageRepository offreStageRepository;
    private final TrelloService trelloService;
    private final ConventionStageService conventionStageService;
    private final ConventionStageRepository conventionStageRepository;
    private final StagiaireResolutionService stagiaireResolutionService;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    public Stage createStage(CreateStageRequest request) {
        Stage stage = StageMapper.toEntity(request);
        stage.setStatut(StatutStage.EN_ATTENTE);
        stage.setStatutSujet(StatutValidation.EN_ATTENTE);

        remplirRelations(stage, request);

        Stage savedStage = stageRepository.save(stage);
        initialiserTrelloPourStage(savedStage);
        Stage persistedStage = stageRepository.save(savedStage);
        notifierEncadrantsAffectes(persistedStage, null, null);
        return persistedStage;
    }

    @Override
    public Stage updateStage(Long id, CreateStageRequest request) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        Long previousEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long previousEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        stage.setTitre(request.getTitre());
        stage.setDateDebut(request.getDateDebut());
        stage.setDateFin(request.getDateFin());
        stage.setDuree(request.getDuree());
        stage.setNbSemaine(request.getNbSemaine());
        stage.setNiveauSouhaite(request.getNiveauSouhaite());
        stage.setSujet(request.getSujet());
        remplirRelations(stage, request);

        Stage updatedStage = stageRepository.save(stage);
        notifierEncadrantsAffectes(updatedStage, previousEncadrantAcademiqueId, previousEncadrantProfessionnelId);
        return updatedStage;
    }

    @Override
    public Stage getStageById(Long id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
    }

    @Override
    public List<Stage> getAllStages() {
        return stageRepository.findAll();
    }

    @Override
    public void deleteStage(Long id) {
        if (!stageRepository.existsById(id)) {
            throw new EntityNotFoundException("Stage introuvable");
        }
        stageRepository.deleteById(id);
    }

    @Override
    public Stage affecterEncadrantAcademique(Long stageId, Long encadrantAcademiqueId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        Long previousEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantAcademiqueId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        synchroniserEncadrantAcademiqueStagiaire(stage.getStagiaire(), encadrant);
        synchroniserStagesActifsDuStagiaire(stage.getStagiaire(), encadrant);
        stage.setEncadrantAcademique(encadrant);
        Stage savedStage = stageRepository.save(stage);
        notifierEncadrantAffecte(
                savedStage,
                previousEncadrantAcademiqueId,
                savedStage.getEncadrantAcademique() != null ? savedStage.getEncadrantAcademique().getId() : null,
                "academique"
        );
        return savedStage;
    }

    @Override
    public Stage affecterEncadrantProfessionnel(Long stageId, Long encadrantProfessionnelId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        Long previousEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        EncadrantProfessionnel encadrant = encadrantProfessionnelRepository.findById(encadrantProfessionnelId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantProfessionnel introuvable"));

        stage.setEncadrantProfessionnel(encadrant);
        Stage savedStage = stageRepository.save(stage);
        notifierEncadrantAffecte(
                savedStage,
                previousEncadrantProfessionnelId,
                savedStage.getEncadrantProfessionnel() != null ? savedStage.getEncadrantProfessionnel().getId() : null,
                "professionnel"
        );
        return savedStage;
    }

    @Override
    public Stage validerStageParEntreprise(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Acces refuse : role responsable entreprise requis.");
        }

        ensureEntrepriseScope(stage, utilisateur);

        stage.setStatut(StatutStage.VALIDE_PAR_ENTREPRISE);
        Stage saved = stageRepository.save(stage);
        notifierValidationEntreprise(saved);
        return saved;
    }

    @Override
    public Stage validerStageParResponsable(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        stage.setStatut(StatutStage.VALIDE_PAR_RESPONSABLE);
        return stageRepository.save(stage);
    }

    @Override
    public List<Stage> getStagesByStagiaire(Long stagiaireId) {
        return stageRepository.findByStagiaireId(stagiaireId);
    }

    @Override
    public List<Stage> getStagesByEntreprise(Long entrepriseId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            ensureEntrepriseScope(entrepriseId, utilisateur);
        }
        return stageRepository.findByEntrepriseId(entrepriseId);
    }

    @Override
    public List<Stage> getStagesByEncadrantAcademique(Long encadrantId) {
        return stageRepository.findByEncadrantAcademiqueId(encadrantId);
    }

    @Override
    public List<Stage> getStagesByEncadrantProfessionnel(Long encadrantId) {
        return stageRepository.findByEncadrantProfessionnelId(encadrantId);
    }

    @Override
    public List<Stage> getStagesPourEncadrantAcademiqueAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : role encadrant academique requis.");
        }
        return stageRepository.findByEncadrantAcademiqueId(utilisateur.getId());
    }

    @Override
    public List<Stage> getStagesPourEncadrantProfessionnelAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_PROFESSIONNEL) {
            throw new RuntimeException("Acces refuse : role encadrant professionnel requis.");
        }
        return stageRepository.findByEncadrantProfessionnelId(utilisateur.getId());
    }

    @Override
    public List<Stage> getStagesPourStagiaireAuthentifie() {
        Stagiaire stagiaire = getAuthenticatedStagiaire();
        return stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(stage -> stage.getStatut() != StatutStage.REFUSE)
                .toList();
    }

    @Override
    public Stage getStageCourantPourStagiaireAuthentifie() {
        List<Stage> stages = getStagesPourStagiaireAuthentifie();

        return stages.stream()
                .filter(stage -> stage.getStatut() == StatutStage.EN_COURS)
                .findFirst()
                .or(() -> stages.stream().filter(stage -> stage.getStatut() == StatutStage.VALIDE_PAR_ENTREPRISE).findFirst())
                .or(() -> stages.stream().filter(stage -> stage.getStatut() == StatutStage.VALIDE_PAR_RESPONSABLE).findFirst())
                .or(() -> stages.stream().filter(stage -> stage.getStatut() == StatutStage.TERMINE).findFirst())
                .orElseThrow(() -> new EntityNotFoundException("Aucun stage ne vous a encore ete affecte."));
    }

    @Override
    public Stage creerStageDepuisOffre(Long offreId, Long stagiaireId) {
        OffreStage offre = offreStageRepository.findById(offreId)
                .orElseThrow(() -> new EntityNotFoundException("Offre introuvable"));

        Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));

        Stage savedStage = creerStageDepuisOffrePourEntreprise(offre, stagiaire, offre.getPublieePar());

        OffreStage offreAssociee = savedStage.getOffreSource();
        if (offreAssociee != null) {
            offreAssociee.setStatut(StatutOffre.FERMEE);
            offreStageRepository.save(offreAssociee);
        }

        return stageRepository.save(savedStage);
    }

    @Override
    public Stage creerStageDepuisOffrePourEntreprise(OffreStage offre, Stagiaire stagiaire, ResponsableEntreprise responsableEntreprise) {
        if (offre == null) {
            throw new EntityNotFoundException("Offre introuvable");
        }

        if (stagiaire == null) {
            throw new EntityNotFoundException("Stagiaire introuvable");
        }

        if (stageRepository.existsByOffreSourceId(offre.getId())) {
            throw new IllegalStateException("Un stage existe deja pour cette offre.");
        }

        Stage stage = new Stage();
        stage.setTitre(offre.getTitre());
        stage.setSujet(offre.getDescriptionMissions());
        stage.setDuree(offre.getDuree());
        stage.setNbSemaine(offre.getDuree() != null ? offre.getDuree() * 4 : null);
        stage.setDateDebut(offre.getDateDebutPrevue());
        stage.setDateFin(calculerDateFin(offre.getDateDebutPrevue(), offre.getDuree()));
        stage.setNiveauSouhaite(offre.getProfilRecherche());
        stage.setEntreprise(offre.getEntreprise());
        stage.setStagiaire(stagiaire);
        stage.setEncadrantAcademique(stagiaire.getEncadrantAcademique());
        stage.setOffreSource(offre);
        stage.setTuteurEntreprise(responsableEntreprise);
        stage.setStatut(StatutStage.EN_ATTENTE);
        stage.setStatutSujet(StatutValidation.EN_ATTENTE);

        Stage savedStage = stageRepository.save(stage);
        initialiserTrelloPourStage(savedStage);
        Stage persistedStage = stageRepository.save(savedStage);
        notifierEncadrantsAffectes(persistedStage, null, null);
        return persistedStage;
    }

    private void remplirRelations(Stage stage, CreateStageRequest request) {
        Stagiaire stagiaire = stagiaireRepository.findById(request.getStagiaireId())
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));
        stage.setStagiaire(stagiaire);

        Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable"));
        stage.setEntreprise(entreprise);

        EncadrantAcademique encadrantAcademique = resoudreEncadrantAcademique(request.getEncadrantAcademiqueId(), stagiaire);
        stage.setEncadrantAcademique(encadrantAcademique);
        synchroniserEncadrantAcademiqueStagiaire(stagiaire, encadrantAcademique);

        if (request.getEncadrantProfessionnelId() != null) {
            EncadrantProfessionnel enc = encadrantProfessionnelRepository.findById(request.getEncadrantProfessionnelId())
                    .orElseThrow(() -> new EntityNotFoundException("EncadrantProfessionnel introuvable"));
            stage.setEncadrantProfessionnel(enc);
        } else {
            stage.setEncadrantProfessionnel(null);
        }

        if (request.getTuteurEntrepriseId() != null) {
            ResponsableEntreprise tuteur = responsableEntrepriseRepository.findById(request.getTuteurEntrepriseId())
                    .orElseThrow(() -> new EntityNotFoundException("TuteurEntreprise introuvable"));
            stage.setTuteurEntreprise(tuteur);
        } else {
            stage.setTuteurEntreprise(null);
        }

        if (request.getOffreSourceId() != null) {
            OffreStage offre = offreStageRepository.findById(request.getOffreSourceId())
                    .orElseThrow(() -> new EntityNotFoundException("Offre introuvable"));
            stage.setOffreSource(offre);
        } else {
            stage.setOffreSource(null);
        }
    }

    private void initialiserTrelloPourStage(Stage stage) {
        System.out.println("Création board Trello pour stage : " + stage.getTitre());

        Map<String, Object> board = trelloService.createBoard("Stage - " + stage.getTitre());

        stage.setTrelloBoardId((String) board.get("id"));
        stage.setTrelloBoardUrl((String) board.get("url"));

        Map<String, Object> todo = trelloService.createList(stage.getTrelloBoardId(), "To Do");
        Map<String, Object> doing = trelloService.createList(stage.getTrelloBoardId(), "In Progress");
        Map<String, Object> done = trelloService.createList(stage.getTrelloBoardId(), "Done");

        stage.setTrelloTodoListId((String) todo.get("id"));
        stage.setTrelloDoingListId((String) doing.get("id"));
        stage.setTrelloDoneListId((String) done.get("id"));
    }

    private LocalDate calculerDateFin(LocalDate dateDebut, Integer dureeMois) {
        if (dateDebut == null || dureeMois == null) {
            return null;
        }

        return dateDebut.plusMonths(dureeMois.longValue());
    }

    private Stagiaire getAuthenticatedStagiaire() {
        String email = getAuthenticatedUtilisateur().getEmail();

        return stagiaireResolutionService.findByEmail(email);
    }

    private Utilisateur getAuthenticatedUtilisateur() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur authentifie introuvable."));
    }

    private void ensureEntrepriseScope(Stage stage, Utilisateur utilisateur) {
        Long stageEntrepriseId = stage.getEntreprise() != null ? stage.getEntreprise().getId() : null;
        ensureEntrepriseScope(stageEntrepriseId, utilisateur);
    }

    private void ensureEntrepriseScope(Long entrepriseId, Utilisateur utilisateur) {
        if (!(utilisateur instanceof ResponsableEntreprise responsableEntreprise)) {
            throw new AccessDeniedException("Acces refuse : compte responsable entreprise requis.");
        }

        Long expectedEntrepriseId = responsableEntreprise.getEntreprise() != null
                ? responsableEntreprise.getEntreprise().getId()
                : null;

        if (expectedEntrepriseId == null || entrepriseId == null || !expectedEntrepriseId.equals(entrepriseId)) {
            throw new AccessDeniedException("Acces refuse a un stage qui n'appartient pas a votre entreprise.");
        }
    }

    private void notifierValidationEntreprise(Stage stage) {
        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            notificationService.creerNotification(
                    stage.getStagiaire().getId(),
                    "Stage validé",
                    "Votre stage " + (stage.getTitre() == null ? "sélectionné" : stage.getTitre()) + " a été validé par votre entreprise.",
                    "VALIDATION_ENTREPRISE",
                    stage.getId(),
                    "STAGE"
            );
        }

        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            notificationService.creerNotification(
                    stage.getEncadrantAcademique().getId(),
                    "Stage validé côté entreprise",
                    "Le stage " + (stage.getTitre() == null ? "sélectionné" : stage.getTitre()) + " a été validé par l'entreprise.",
                    "VALIDATION_ENTREPRISE",
                    stage.getId(),
                    "STAGE"
            );
        }
    }

    private EncadrantAcademique resoudreEncadrantAcademique(Long encadrantAcademiqueId, Stagiaire stagiaire) {
        if (encadrantAcademiqueId != null) {
            return encadrantAcademiqueRepository.findById(encadrantAcademiqueId)
                    .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));
        }

        return stagiaire != null ? stagiaire.getEncadrantAcademique() : null;
    }

    private void synchroniserEncadrantAcademiqueStagiaire(Stagiaire stagiaire, EncadrantAcademique encadrant) {
        if (stagiaire == null || stagiaire.getId() == null || encadrant == null) {
            return;
        }

        Long currentId = stagiaire.getEncadrantAcademique() != null ? stagiaire.getEncadrantAcademique().getId() : null;
        if (encadrant.getId().equals(currentId)) {
            return;
        }

        stagiaire.setEncadrantAcademique(encadrant);
        stagiaireRepository.save(stagiaire);
    }

    private void synchroniserStagesActifsDuStagiaire(Stagiaire stagiaire, EncadrantAcademique encadrant) {
        if (stagiaire == null || stagiaire.getId() == null || encadrant == null) {
            return;
        }

        List<Stage> stagesActifs = stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(this::isStageActifPourEncadrant)
                .toList();

        boolean updated = false;
        for (Stage current : stagesActifs) {
            Long currentId = current.getEncadrantAcademique() != null ? current.getEncadrantAcademique().getId() : null;
            if (!encadrant.getId().equals(currentId)) {
                current.setEncadrantAcademique(encadrant);
                updated = true;
            }
        }

        if (updated) {
            stageRepository.saveAll(stagesActifs);
        }
    }

    private boolean isStageActifPourEncadrant(Stage stage) {
        return stage != null
                && stage.getStatut() != null
                && stage.getStatut() != StatutStage.REFUSE
                && stage.getStatut() != StatutStage.TERMINE;
    }

    private void notifierEncadrantsAffectes(Stage stage,
                                            Long previousEncadrantAcademiqueId,
                                            Long previousEncadrantProfessionnelId) {
        Long currentEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long currentEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        notifierEncadrantAffecte(stage, previousEncadrantAcademiqueId, currentEncadrantAcademiqueId, "academique");
        notifierEncadrantAffecte(stage, previousEncadrantProfessionnelId, currentEncadrantProfessionnelId, "professionnel");
    }

    private void notifierEncadrantAffecte(Stage stage,
                                          Long previousEncadrantId,
                                          Long currentEncadrantId,
                                          String typeEncadrant) {
        if (currentEncadrantId == null || currentEncadrantId.equals(previousEncadrantId)) {
            return;
        }

        notificationService.creerNotification(
                currentEncadrantId,
                "Affectation encadrant",
                "Vous avez ete affecte comme encadrant " + typeEncadrant + " pour un stage.",
                "AFFECTATION_ENCADRANT_STAGE",
                stage.getId(),
                "STAGE"
        );
    }

    @Override
    public Stage validerSujetParEncadrantAcademique(Long stageId, Long encadrantId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        if (stage.getEncadrantAcademique() == null || !stage.getEncadrantAcademique().getId().equals(encadrantId)) {
            throw new RuntimeException("Cet encadrant académique n'est pas affecté à ce stage");
        }

        stage.setSujetValidePar(encadrant);
        stage.setStatutSujet(StatutValidation.VALIDEE);
        stage.setStatut(StatutStage.EN_COURS);

        Stage saved = stageRepository.save(stage);
        genererConventionSiAbsente(saved);
        return saved;
    }

    @Override
    public Stage validerSujetParEncadrantAcademiqueAuthentifie(Long stageId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : seul l'encadrant academique affecte peut valider le sujet.");
        }
        return validerSujetParEncadrantAcademique(stageId, utilisateur.getId());
    }

    private void genererConventionSiAbsente(Stage stage) {
        if (stage == null || stage.getId() == null) return;
        if (conventionStageRepository.existsByStageId(stage.getId())) return;

        conventionStageService.createByStage(stage.getId(), new ConventionStageDto());
    }

    @Override
    public Stage refuserSujetParEncadrantAcademique(Long stageId, Long encadrantId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        if (stage.getEncadrantAcademique() == null || !stage.getEncadrantAcademique().getId().equals(encadrantId)) {
            throw new RuntimeException("Cet encadrant académique n'est pas affecté à ce stage");
        }

        stage.setSujetValidePar(encadrant);
        stage.setStatutSujet(StatutValidation.REFUSEE);
        stage.setStatut(StatutStage.EN_ATTENTE);

        return stageRepository.save(stage);
    }

    @Override
    public Stage refuserSujetParEncadrantAcademiqueAuthentifie(Long stageId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : seul l'encadrant academique affecte peut refuser le sujet.");
        }
        return refuserSujetParEncadrantAcademique(stageId, utilisateur.getId());
    }

    @Override
    public Map<String, Object> createTrelloBoardIfNotExists(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        authorizeLinkedStageAccess(stage);

        if (hasText(stage.getTrelloBoardId())) {
            if (!hasText(stage.getTrelloBoardUrl())) {
                stage.setTrelloBoardUrl("https://trello.com/b/" + stage.getTrelloBoardId());
                stage = stageRepository.save(stage);
            }
            return buildTrelloBoardResponse(stage, false);
        }

        try {
            String boardName = buildTrelloBoardName(stage);
            Map<String, Object> board = trelloService.createBoard(boardName);
            if (board == null || board.get("id") == null) {
                throw new IllegalStateException("Réponse Trello invalide lors de la création du board.");
            }

            stage.setTrelloBoardId(String.valueOf(board.get("id")));
            stage.setTrelloBoardUrl(String.valueOf(board.getOrDefault("url", "")));

            Map<String, Object> todo = trelloService.createList(stage.getTrelloBoardId(), "To Do");
            Map<String, Object> doing = trelloService.createList(stage.getTrelloBoardId(), "In Progress");
            Map<String, Object> done = trelloService.createList(stage.getTrelloBoardId(), "Done");

            stage.setTrelloTodoListId(readTrelloId(todo));
            stage.setTrelloDoingListId(readTrelloId(doing));
            stage.setTrelloDoneListId(readTrelloId(done));

            Stage saved = stageRepository.save(stage);
            return buildTrelloBoardResponse(saved, true);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Erreur lors de la création du board Trello", ex);
        }
    }

    @Override
    public Map<String, Object> getResumeTrelloStage(Long stageId) {
        createTrelloBoardIfNotExists(stageId);
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        List<Map<String, Object>> listes = trelloService.getListsByBoard(stage.getTrelloBoardId());
        List<Map<String, Object>> cartes = trelloService.getCardsByBoard(stage.getTrelloBoardId());

        String doneListId = stage.getTrelloDoneListId();
        String doingListId = stage.getTrelloDoingListId();
        String todoListId = stage.getTrelloTodoListId();

        List<Map<String, Object>> tachesTerminees = cartes.stream()
                .filter(c -> doneListId != null && doneListId.equals(c.get("idList")))
                .toList();

        List<Map<String, Object>> tachesEnCours = cartes.stream()
                .filter(c -> doingListId != null && doingListId.equals(c.get("idList")))
                .toList();

        List<Map<String, Object>> tachesAFaire = cartes.stream()
                .filter(c -> todoListId != null && todoListId.equals(c.get("idList")))
                .toList();

        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("stageId", stage.getId());
        resume.put("titreStage", stage.getTitre());
        resume.put("sujet", stage.getSujet());
        resume.put("trelloBoardId", stage.getTrelloBoardId());
        resume.put("trelloBoardUrl", stage.getTrelloBoardUrl());

        resume.put("nombreTotalTaches", cartes.size());
        resume.put("nombreTachesAFaire", tachesAFaire.size());
        resume.put("nombreTachesEnCours", tachesEnCours.size());
        resume.put("nombreTachesTerminees", tachesTerminees.size());

        resume.put("listes", listes);
        resume.put("tachesAFaire", tachesAFaire);
        resume.put("tachesEnCours", tachesEnCours);
        resume.put("tachesTerminees", tachesTerminees);

        return resume;
    }

    private Map<String, Object> buildTrelloBoardResponse(Stage stage, boolean created) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stageId", stage.getId());
        response.put("trelloBoardId", stage.getTrelloBoardId());
        response.put("trelloBoardUrl", stage.getTrelloBoardUrl());
        response.put("trelloTodoListId", stage.getTrelloTodoListId());
        response.put("trelloDoingListId", stage.getTrelloDoingListId());
        response.put("trelloDoneListId", stage.getTrelloDoneListId());
        response.put("created", created);
        return response;
    }

    private String buildTrelloBoardName(Stage stage) {
        String studentName = stage.getStagiaire() == null
                ? "Stagiaire"
                : buildFullName(stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom());
        String companyName = stage.getEntreprise() == null || !hasText(stage.getEntreprise().getNom())
                ? "Entreprise"
                : stage.getEntreprise().getNom();
        return "Stage - " + studentName + " - " + companyName;
    }

    private String buildFullName(String prenom, String nom) {
        String fullName = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return hasText(fullName) ? fullName : "Stagiaire";
    }

    private String readTrelloId(Map<String, Object> value) {
        return value == null || value.get("id") == null ? null : String.valueOf(value.get("id"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void authorizeLinkedStageAccess(Stage stage) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        Long userId = utilisateur.getId();
        Role role = utilisateur.getRole();

        boolean allowed = switch (role) {
            case STAGIAIRE -> stage.getStagiaire() != null && userId.equals(stage.getStagiaire().getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && userId.equals(stage.getEncadrantAcademique().getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && userId.equals(stage.getEncadrantProfessionnel().getId());
            default -> false;
        };

        if (!allowed) {
            throw new AccessDeniedException("Acces refuse : ce stage n'est pas lie a votre compte.");
        }
    }

    @Override
    public Map<String, Object> genererRapportStage(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        if (stage.getDateFin() == null) {
            throw new RuntimeException("La date de fin du stage est absente");
        }

        if (stage.getDateFin().isAfter(LocalDate.now())) {
            throw new RuntimeException("Le rapport ne peut être généré qu'à la fin du stage");
        }

        return getResumeTrelloStage(stageId);
    }
}
