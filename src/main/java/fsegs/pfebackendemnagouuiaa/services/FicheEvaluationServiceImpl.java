package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.PartieEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.FicheEvaluationMapper;
import fsegs.pfebackendemnagouuiaa.repository.CritereEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.NoteAttribueeRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FicheEvaluationServiceImpl implements FicheEvaluationService {

    private static final String FICHE_INTROUVABLE = "Fiche d'evaluation introuvable.";
    private static final String STAGE_INTROUVABLE = "Stage introuvable.";
    private static final String SIGNATURE_MANQUANTE = "Veuillez enregistrer votre signature dans votre profil avant de continuer.";
    private static final String FICHE_VERROUILLEE = "La fiche d'evaluation est deja verrouillee et ne peut plus etre modifiee.";
    private static final String PARTIE_PROFESSIONNELLE_DEJA_SOUMISE =
            "Votre evaluation a deja ete soumise et signee. Cette partie est maintenant en lecture seule.";
    private static final String PARTIE_ENTREPRISE_DEJA_SOUMISE =
            "Votre evaluation a deja ete soumise et signee. Cette partie est maintenant en lecture seule.";
    private static final String UTILISATEUR_NON_AUTHENTIFIE = "Utilisateur authentifie introuvable.";
    private static final String EVALUATION_INDISPONIBLE_MESSAGE =
            EvaluationStageAccessRules.UNAVAILABLE_MESSAGE;

    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final StageRepository stageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FicheEvaluationMapper ficheEvaluationMapper;
    private final CritereEvaluationRepository critereEvaluationRepository;
    private final NoteAttribueeRepository noteAttribueeRepository;
    private final EvaluationSheetBootstrapService evaluationSheetBootstrapService;
    private final JwtService jwtService;

    @Override
    @Transactional
    public FicheEvaluationDto create(FicheEvaluationDto dto) {
        if (dto.getStageId() == null) {
            throw new IllegalArgumentException("Le stage est obligatoire.");
        }
        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new EntityNotFoundException(STAGE_INTROUVABLE));
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        ensureCanManageEvaluation(stage, utilisateur);
        ensureEvaluationAllowed(stage);
        if (ficheEvaluationRepository.existsByStageId(stage.getId())) {
            throw new IllegalArgumentException("Une fiche d'evaluation existe deja pour ce stage.");
        }

        FicheEvaluation entity = evaluationSheetBootstrapService.ensureSheetExists(stage.getId());
        return toDtoForApi(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public FicheEvaluationDto getById(Long id) {
        FicheEvaluation fiche = getFicheOrThrow(id);
        ensureCanViewFiche(fiche, getAuthenticatedUtilisateur());
        return toDtoForApi(fiche);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FicheEvaluationDto> getAll() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        return ficheEvaluationRepository.findAll().stream()
                .filter(fiche -> canViewFiche(fiche, utilisateur))
                .map(this::toDtoForApi)
                .toList();
    }

    @Override
    @Transactional
    public Optional<FicheEvaluationDto> findByStageIdIfPresent(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException(STAGE_INTROUVABLE));
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        bootstrapEvaluationSheetIfEvaluator(stage, utilisateur);

        return ficheEvaluationRepository.findFirstByStageId(stageId)
                .map(fiche -> {
                    ensureCanViewFiche(fiche, utilisateur);
                    return toDtoForApi(fiche);
                });
    }

    @Override
    @Transactional
    public FicheEvaluationDto getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException(STAGE_INTROUVABLE));
        if (!EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
            throw new BusinessException(EVALUATION_INDISPONIBLE_MESSAGE);
        }
        return findByStageIdIfPresent(stageId).orElseThrow(() ->
                new EntityNotFoundException(FICHE_INTROUVABLE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FicheEvaluationDto> getByReunionFinaleId(Long reunionFinaleId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        return ficheEvaluationRepository.findByReunionFinaleId(reunionFinaleId).stream()
                .filter(fiche -> canViewFiche(fiche, utilisateur))
                .map(this::toDtoForApi)
                .toList();
    }

    @Override
    @Transactional
    public FicheEvaluationDto update(Long id, FicheEvaluationDto dto) {
        FicheEvaluation fiche = getFicheOrThrow(id);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        ensureEvaluationAllowed(requireStage(fiche));

        if (isProfessionalSupervisor(fiche.getStage(), utilisateur)) {
            return remplirPartieEncadrantProfessionnel(id, utilisateur.getId(), dto);
        }
        if (isCompanyRepresentative(fiche.getStage(), utilisateur)) {
            return remplirPartieResponsableEntreprise(id, utilisateur.getId(), dto);
        }

        throw new AccessDeniedException("Utilisateur non autorise a modifier cette fiche d'evaluation.");
    }

    @Override
    @Transactional
    public FicheEvaluationDto signerFiche(Long ficheId, Long userId) {
        FicheEvaluation fiche = getFicheOrThrow(ficheId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        Stage stage = requireStage(fiche);
        ensureEvaluationAllowed(stage);

        if (isProfessionalSupervisor(stage, utilisateur)) {
            StageDocumentSignatureRules.ensureEvaluationSigningAllowed(
                    fiche, fiche.pretPourSignatureEncadrantProfessionnel());
            // Idempotence : déjà signé → retour sans erreur
            if (fiche.estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL)) {
                return toDtoForApi(fiche);
            }
            // E4 — signature absente du profil
            requireSavedSignature(utilisateur);

            Signature sig = new Signature();
            sig.setRoleSignature(RoleSignature.ENCADRANT_PROFESSIONNEL);
            sig.setSignataireId(utilisateur.getId());
            sig.setDateSignature(LocalDateTime.now());
            sig.setUrlSignature(utilisateur.getUrlSignature());
            fiche.getSignatures().add(sig);

        } else if (isCompanyRepresentative(stage, utilisateur)) {
            StageDocumentSignatureRules.ensureEvaluationSigningAllowed(
                    fiche, fiche.pretPourSignatureResponsableEntreprise());
            if (fiche.estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE)) {
                return toDtoForApi(fiche);
            }
            requireSavedSignature(utilisateur);

            Signature sig = new Signature();
            sig.setRoleSignature(RoleSignature.RESPONSABLE_ENTREPRISE);
            sig.setSignataireId(utilisateur.getId());
            sig.setDateSignature(LocalDateTime.now());
            sig.setUrlSignature(utilisateur.getUrlSignature());
            fiche.getSignatures().add(sig);

        } else {
            throw new AccessDeniedException("Utilisateur non autorise a signer cette fiche d'evaluation.");
        }

        recalculateAndPersistFinalScore(fiche);
        FicheEvaluation ficheSauvegardee = ficheEvaluationRepository.findById(fiche.getId()).orElse(fiche);
        if (ficheSauvegardee.estVerrouillee()) {
            cloturerStageApresValidationComplete(ficheSauvegardee.getStage());
        }
        return toDtoForApi(ficheSauvegardee);
    }

    private void cloturerStageApresValidationComplete(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return;
        }
        if (stage.getStatut() == StatutStage.TERMINE) {
            return;
        }
        stage.setStatut(StatutStage.TERMINE);
        stageRepository.save(stage);
    }

    @Override
    @Transactional
    public FicheEvaluationDto remplirPartieEncadrantProfessionnel(Long ficheId, Long userId, FicheEvaluationDto dto) {
        FicheEvaluation fiche = resolveFicheForEvaluatorEdit(ficheId, userId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationAllowed(requireStage(fiche));

        if (!isProfessionalSupervisor(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul l'encadrant professionnel affecte au stage peut renseigner cette partie.");
        }
        ensureSignerCanStillEdit(fiche, RoleSignature.ENCADRANT_PROFESSIONNEL, PARTIE_PROFESSIONNELLE_DEJA_SOUMISE);

        validateRequiredText(dto.getPointFortEncadrantPro(), "Les points forts de l'encadrant professionnel sont obligatoires.");
        validateRequiredText(dto.getAxeAmeliorationEncadrantPro(), "Les axes d'amelioration de l'encadrant professionnel sont obligatoires.");

        fiche.setPointFortEncadrantPro(dto.getPointFortEncadrantPro().trim());
        fiche.setAxeAmeliorationEncadrantPro(dto.getAxeAmeliorationEncadrantPro().trim());
        ficheEvaluationRepository.save(fiche);
        recalculateAndPersistFinalScore(fiche);

        return toDtoForApi(fiche);
    }

    @Override
    @Transactional
    public FicheEvaluationDto remplirPartieResponsableEntreprise(Long ficheId, Long userId, FicheEvaluationDto dto) {
        FicheEvaluation fiche = resolveFicheForEvaluatorEdit(ficheId, userId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationAllowed(requireStage(fiche));

        if (!isCompanyRepresentative(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul le representant de l'entreprise liee au stage peut renseigner cette partie.");
        }
        ensureSignerCanStillEdit(fiche, RoleSignature.RESPONSABLE_ENTREPRISE, PARTIE_ENTREPRISE_DEJA_SOUMISE);

        validateRequiredText(dto.getPointFortResponsableEntreprise(), "Les points forts du representant de l'entreprise sont obligatoires.");
        validateRequiredText(dto.getAxeAmeliorationResponsableEntreprise(), "Les axes d'amelioration du representant de l'entreprise sont obligatoires.");

        fiche.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise().trim());
        fiche.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise().trim());
        ficheEvaluationRepository.save(fiche);
        recalculateAndPersistFinalScore(fiche);

        return toDtoForApi(fiche);
    }

    @Override
    @Transactional
    public FicheEvaluationDto enregistrerNotesEncadrantProfessionnel(Long ficheId, Long userId, List<NoteAttribueeDto> notes) {
        FicheEvaluation fiche = resolveFicheForEvaluatorEdit(ficheId, userId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationAllowed(requireStage(fiche));

        if (!isProfessionalSupervisor(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul l'encadrant professionnel affecte au stage peut saisir les notes.");
        }
        ensureSignerCanStillEdit(fiche, RoleSignature.ENCADRANT_PROFESSIONNEL, PARTIE_PROFESSIONNELLE_DEJA_SOUMISE);
        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Au moins une note doit etre renseignee.");
        }

        for (NoteAttribueeDto noteDto : notes) {
            upsertProfessionalNote(fiche, noteDto);
        }

        recalculateAndPersistFinalScore(fiche);
        return toDtoForApi(fiche);
    }

    // ── Responsable Entreprise : enregistrement notes (Ponctualite, etc.) ─────

    @Override
    @Transactional
    public FicheEvaluationDto enregistrerNotesResponsableEntreprise(Long ficheId, Long userId, List<NoteAttribueeDto> notes) {
        FicheEvaluation fiche = resolveFicheForEvaluatorEdit(ficheId, userId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationAllowed(requireStage(fiche));

        if (!isCompanyRepresentative(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul le representant de l'entreprise affecte au stage peut saisir ses notes.");
        }
        ensureSignerCanStillEdit(fiche, RoleSignature.RESPONSABLE_ENTREPRISE, PARTIE_ENTREPRISE_DEJA_SOUMISE);
        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Au moins une note doit etre renseignee.");
        }

        for (NoteAttribueeDto noteDto : notes) {
            upsertCompanyRepresentativeNote(fiche, noteDto);
        }

        recalculateAndPersistFinalScore(fiche);
        return toDtoForApi(fiche);
    }

    private void upsertCompanyRepresentativeNote(FicheEvaluation fiche, NoteAttribueeDto dto) {
        upsertRoleNote(fiche, dto, PartieEvaluation.RESPONSABLE_ENTREPRISE);
    }

    private void upsertProfessionalNote(FicheEvaluation fiche, NoteAttribueeDto dto) {
        upsertRoleNote(fiche, dto, PartieEvaluation.ENCADRANT_PROFESSIONNEL);
    }

    private void upsertRoleNote(FicheEvaluation fiche, NoteAttribueeDto dto, PartieEvaluation partie) {
        validateNote(dto);

        CritereEvaluation critere = resolveCriterionForFiche(fiche, dto, partie);
        CleNoteAttribuee id = new CleNoteAttribuee(fiche.getId(), critere.getId());

        NoteAttribuee note = noteAttribueeRepository.findById(id)
                .orElseGet(() -> {
                    NoteAttribuee entity = new NoteAttribuee();
                    entity.setId(id);
                    entity.setFicheEvaluation(fiche);
                    entity.setCritereEvaluation(critere);
                    return entity;
                });

        note.setPoids(dto.getPoids());
        note.setBareme(dto.getBareme() != null && dto.getBareme() > 0 ? dto.getBareme() : 5);
        note.setNote(dto.getNote());
        note.setCommentaire(normalizeNoteCommentaire(dto.getCommentaire()));
        noteAttribueeRepository.saveAndFlush(note);
    }

    private CritereEvaluation resolveCriterionForFiche(
            FicheEvaluation fiche,
            NoteAttribueeDto dto,
            PartieEvaluation partie) {
        if (dto.getCritereEvaluationId() != null) {
            CritereEvaluation critere = critereEvaluationRepository.findById(dto.getCritereEvaluationId())
                    .orElseThrow(() -> new EntityNotFoundException("Critere d'evaluation introuvable."));
            if (critere.getFiche() != null && !critere.getFiche().getId().equals(fiche.getId())) {
                throw new IllegalArgumentException("Le critere d'evaluation ne correspond pas a cette fiche.");
            }
            return critere;
        }

        validateRequiredText(dto.getCritereLibelle(), "Le libelle du critere d'evaluation est obligatoire.");
        String libelle = dto.getCritereLibelle().trim();

        return critereEvaluationRepository.findByFicheId(fiche.getId()).stream()
                .filter(c -> c.getPartie() == partie)
                .filter(c -> EvaluationCriteriaCatalog.matchesCriterionLabel(c.getLibelle(), libelle))
                .findFirst()
                .orElseGet(() -> createCriterionForFiche(fiche, libelle, partie));
    }

    private CritereEvaluation createCriterionForFiche(
            FicheEvaluation fiche,
            String libelle,
            PartieEvaluation partie) {
        CritereEvaluation critere = new CritereEvaluation();
        critere.setLibelle(libelle);
        critere.setDescription(libelle);
        critere.setCategorie("Evaluation du stage");
        critere.setBareme(5);
        critere.setConsigne(null);
        critere.setPartie(partie);
        critere.setFiche(fiche);
        return critereEvaluationRepository.save(critere);
    }

    private String normalizeNoteCommentaire(String commentaire) {
        if (commentaire == null) {
            return null;
        }
        String trimmed = commentaire.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Recalcule la note finale à partir du dépôt (sans manipuler {@code notesAttribuees})
     * pour éviter la suppression accidentelle de notes avec {@code orphanRemoval = true}.
     */
    private void recalculateAndPersistFinalScore(FicheEvaluation fiche) {
        if (fiche == null || fiche.getId() == null) {
            return;
        }
        noteAttribueeRepository.flush();
        List<NoteAttribuee> notes = noteAttribueeRepository.findByFicheEvaluationId(fiche.getId());
        fiche.setNoteFinale(computeFinalScoreFromNotes(notes));
        ficheEvaluationRepository.save(fiche);
    }

    private static double computeFinalScoreFromNotes(List<NoteAttribuee> notes) {
        if (notes == null || notes.isEmpty()) {
            return 0.0;
        }
        var evaluees = notes.stream()
                .filter(NoteAttribuee::estEvalue)
                .toList();
        if (evaluees.isEmpty()) {
            return 0.0;
        }
        double averageOnFive = evaluees.stream()
                .mapToDouble(n -> {
                    int bareme = (n.getBareme() != null && n.getBareme() > 0) ? n.getBareme() : 5;
                    return (n.getNote() / (double) bareme) * 5.0;
                })
                .average()
                .orElse(0.0);
        return Math.round(averageOnFive * 10.0) / 10.0;
    }

    private void validateNote(NoteAttribueeDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Les donnees de note sont obligatoires.");
        }
        if (dto.getPoids() == null || dto.getPoids() <= 0) {
            throw new IllegalArgumentException("Le coefficient doit etre superieur a zero.");
        }
        int bareme = (dto.getBareme() != null && dto.getBareme() > 0) ? dto.getBareme() : 5;
        if (dto.getNote() == null || dto.getNote() < 0 || dto.getNote() > bareme) {
            throw new IllegalArgumentException("La note attribuee doit etre comprise entre 0 et " + bareme + ".");
        }
    }

    private FicheEvaluation getFicheOrThrow(Long ficheId) {
        return ficheEvaluationRepository.findById(ficheId)
                .orElseThrow(() -> new EntityNotFoundException(FICHE_INTROUVABLE));
    }

    private Stage requireStage(FicheEvaluation fiche) {
        if (fiche.getStage() == null) {
            throw new EntityNotFoundException(STAGE_INTROUVABLE);
        }
        return fiche.getStage();
    }

    private void ensureEvaluationAllowed(Stage stage) {
        EvaluationStageAccessRules.ensureEvaluationPeriodOpen(stage);
    }

    private void bootstrapEvaluationSheetIfEvaluator(Stage stage, Utilisateur utilisateur) {
        if (stage == null || stage.getId() == null || utilisateur == null) {
            return;
        }
        if (!EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
            return;
        }
        if (!isProfessionalSupervisor(stage, utilisateur) && !isCompanyRepresentative(stage, utilisateur)) {
            return;
        }
        evaluationSheetBootstrapService.ensureSheetExists(stage.getId());
    }

    private FicheEvaluation resolveFicheForEvaluatorEdit(Long ficheId, Long userId) {
        FicheEvaluation fiche = getFicheOrThrow(ficheId);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        Stage stage = requireStage(fiche);
        ensureCanManageEvaluation(stage, utilisateur);
        bootstrapEvaluationSheetIfEvaluator(stage, utilisateur);
        return getFicheOrThrow(ficheId);
    }

    private FicheEvaluationDto toDtoForApi(FicheEvaluation fiche) {
        Stage stage = requireStage(fiche);
        if (!EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
            return buildEmptyEvaluationView(stage, fiche);
        }
        FicheEvaluationDto dto = ficheEvaluationMapper.toDto(fiche);
        dto.setEvaluationAccessible(Boolean.TRUE);
        dto.setEvaluationIndisponibleMessage(null);
        return dto;
    }

    private FicheEvaluationDto buildEmptyEvaluationView(Stage stage, FicheEvaluation fiche) {
        FicheEvaluationDto dto = new FicheEvaluationDto();
        dto.setId(fiche.getId());
        dto.setStageId(stage.getId());
        dto.setStageTitre(stage.getTitre());
        dto.setStageSujet(stage.getSujet());
        dto.setStageDateDebut(stage.getDateDebut());
        dto.setStageDateFin(EvaluationStageAccessRules.resolveDateFin(stage));
        if (stage.getStagiaire() != null) {
            dto.setStagiaireNomComplet(
                    (stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom()).trim()
            );
        }
        if (stage.getEntreprise() != null) {
            dto.setEntrepriseNom(stage.getEntreprise().getNom());
        }
        dto.setPointFortEncadrantPro("");
        dto.setAxeAmeliorationEncadrantPro("");
        dto.setPointFortResponsableEntreprise("");
        dto.setAxeAmeliorationResponsableEntreprise("");
        dto.setNoteFinale(null);
        dto.setNotesAttribuees(new java.util.ArrayList<>());
        dto.setSignatures(new java.util.ArrayList<>());
        dto.setDonneesCompletes(Boolean.FALSE);
        dto.setPretSignatureEncadrantProfessionnel(Boolean.FALSE);
        dto.setPretSignatureResponsableEntreprise(Boolean.FALSE);
        dto.setSignaturesCompletes(Boolean.FALSE);
        dto.setComplete(Boolean.FALSE);
        dto.setVerrouillee(Boolean.FALSE);
        dto.setEvaluationAccessible(Boolean.FALSE);
        dto.setEvaluationIndisponibleMessage(EVALUATION_INDISPONIBLE_MESSAGE);
        return dto;
    }

    private Utilisateur getAuthenticatedUtilisateur() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException(UTILISATEUR_NON_AUTHENTIFIE));
    }

    private Utilisateur getAuthorizedUser(Long userId) {
        Utilisateur authenticated = getAuthenticatedUtilisateur();
        if (userId == null || !authenticated.getId().equals(userId)) {
            throw new AccessDeniedException("Utilisateur non autorise.");
        }
        return utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable."));
    }

    private void ensureEditable(FicheEvaluation fiche) {
        if (fiche.estVerrouillee()) {
            throw new IllegalStateException(FICHE_VERROUILLEE);
        }
    }

    private void ensureSignerCanStillEdit(FicheEvaluation fiche, RoleSignature roleSignature, String message) {
        if (fiche != null && roleSignature != null && fiche.estSignePar(roleSignature)) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureCanViewFiche(FicheEvaluation fiche, Utilisateur utilisateur) {
        if (!canViewFiche(fiche, utilisateur)) {
            throw new AccessDeniedException("Acces non autorise a cette fiche d'evaluation.");
        }
    }

    private boolean canViewFiche(FicheEvaluation fiche, Utilisateur utilisateur) {
        if (utilisateur == null || fiche == null || fiche.getStage() == null) {
            return false;
        }

        Stage stage = fiche.getStage();
        return switch (utilisateur.getRole()) {
            case ADMINISTRATEUR,
                    RESPONSABLE_STAGE -> true;
            case ENCADRANT_PROFESSIONNEL -> isProfessionalSupervisor(stage, utilisateur);
            case RESPONSABLE_ENTREPRISE -> isCompanyRepresentative(stage, utilisateur);
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null
                    && stage.getEncadrantAcademique().getId().equals(utilisateur.getId());
            case STAGIAIRE -> stage.getStagiaire() != null && stage.getStagiaire().getId().equals(utilisateur.getId());
            default -> false;
        };
    }

    private void ensureCanManageEvaluation(Stage stage, Utilisateur utilisateur) {
        if (!(isProfessionalSupervisor(stage, utilisateur) || isCompanyRepresentative(stage, utilisateur))) {
            throw new AccessDeniedException("Utilisateur non autorise a gerer la fiche d'evaluation de ce stage.");
        }
    }

    private boolean isProfessionalSupervisor(Stage stage, Utilisateur utilisateur) {
        return utilisateur != null
                && utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage != null
                && stage.getEncadrantProfessionnel() != null
                && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
    }

    private boolean isCompanyRepresentative(Stage stage, Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE || stage == null) {
            return false;
        }
        ResponsableEntreprise re = (ResponsableEntreprise) utilisateur;
        return re.getEntreprise() != null
                && stage.getEntreprise() != null
                && re.getEntreprise().getId().equals(stage.getEntreprise().getId());
    }

    private void validateRequiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireSavedSignature(Utilisateur utilisateur) {
        String sig = utilisateur.getUrlSignature();
        if (sig == null || sig.trim().isEmpty()) {
            throw new IllegalStateException(SIGNATURE_MANQUANTE);
        }
    }

}
