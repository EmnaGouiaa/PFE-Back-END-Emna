package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.CleNoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
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
    private static final String UTILISATEUR_NON_AUTHENTIFIE = "Utilisateur authentifie introuvable.";

    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final StageRepository stageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FicheEvaluationMapper ficheEvaluationMapper;
    private final CritereEvaluationRepository critereEvaluationRepository;
    private final NoteAttribueeRepository noteAttribueeRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public FicheEvaluationDto create(FicheEvaluationDto dto) {
        if (dto.getStageId() == null) {
            throw new IllegalArgumentException("Le stage est obligatoire.");
        }
        if (dto.getReunionFinaleId() == null) {
            throw new IllegalArgumentException("La reunion finale est obligatoire.");
        }

        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new EntityNotFoundException(STAGE_INTROUVABLE));
        ensureCanManageEvaluation(stage, getAuthenticatedUtilisateur());

        ReunionFinale reunionFinale = reunionFinaleRepository.findById(dto.getReunionFinaleId())
                .orElseThrow(() -> new EntityNotFoundException("Reunion finale introuvable."));

        if (reunionFinale.getStage() == null || !reunionFinale.getStage().getId().equals(stage.getId())) {
            throw new IllegalArgumentException("La reunion finale selectionnee ne correspond pas a ce stage.");
        }
        ensureEvaluationSectionOpen(stage);
        if (ficheEvaluationRepository.existsByStageId(stage.getId())) {
            throw new IllegalArgumentException("Une fiche d'evaluation existe deja pour ce stage.");
        }

        FicheEvaluation entity = ficheEvaluationMapper.toEntity(dto);
        entity.setStage(stage);
        entity.setReunionFinale(reunionFinale);
        entity.setNoteFinale(0.0);

        return ficheEvaluationMapper.toDto(ficheEvaluationRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public FicheEvaluationDto getById(Long id) {
        FicheEvaluation fiche = getFicheOrThrow(id);
        ensureCanViewFiche(fiche, getAuthenticatedUtilisateur());
        return ficheEvaluationMapper.toDto(fiche);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FicheEvaluationDto> getAll() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        return ficheEvaluationRepository.findAll().stream()
                .filter(fiche -> canViewFiche(fiche, utilisateur))
                .map(ficheEvaluationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FicheEvaluationDto> findByStageIdIfPresent(Long stageId) {
        return ficheEvaluationRepository.findFirstByStageId(stageId)
                .map(fiche -> {
                    ensureCanViewFiche(fiche, getAuthenticatedUtilisateur());
                    return ficheEvaluationMapper.toDto(fiche);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public FicheEvaluationDto getByStageId(Long stageId) {
        return findByStageIdIfPresent(stageId).orElseThrow(() ->
                new EntityNotFoundException(FICHE_INTROUVABLE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FicheEvaluationDto> getByReunionFinaleId(Long reunionFinaleId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        return ficheEvaluationRepository.findByReunionFinaleId(reunionFinaleId).stream()
                .filter(fiche -> canViewFiche(fiche, utilisateur))
                .map(ficheEvaluationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public FicheEvaluationDto update(Long id, FicheEvaluationDto dto) {
        FicheEvaluation fiche = getFicheOrThrow(id);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        ensureEvaluationSectionOpen(requireStage(fiche));

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
        ensureEvaluationSectionOpen(stage);

        if (isProfessionalSupervisor(stage, utilisateur)) {
            if (!fiche.partieEncadrantProfessionnelComplete()) {
                throw new IllegalStateException("La partie de l'encadrant professionnel est incomplete.");
            }
            if (!fiche.toutesLesNotesSontRenseignees()) {
                throw new IllegalStateException("Toutes les notes d'evaluation doivent etre renseignees avant la signature.");
            }
            // Idempotence : déjà signé → retour sans erreur
            if (fiche.estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL)) {
                return ficheEvaluationMapper.toDto(fiche);
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
            if (!fiche.partieResponsableEntrepriseComplete()) {
                throw new IllegalStateException("La partie du representant de l'entreprise est incomplete.");
            }
            if (fiche.estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE)) {
                return ficheEvaluationMapper.toDto(fiche);
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

        fiche.setNoteFinale(fiche.calculerNoteFinale());
        FicheEvaluation ficheSauvegardee = ficheEvaluationRepository.save(fiche);
        if (ficheSauvegardee.estVerrouillee()) {
            cloturerStageApresValidationComplete(ficheSauvegardee.getStage());
        }
        return ficheEvaluationMapper.toDto(ficheSauvegardee);
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
        FicheEvaluation fiche = getFicheOrThrow(ficheId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationSectionOpen(requireStage(fiche));

        if (!isProfessionalSupervisor(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul l'encadrant professionnel affecte au stage peut renseigner cette partie.");
        }

        validateRequiredText(dto.getPointFortEncadrantPro(), "Les points forts de l'encadrant professionnel sont obligatoires.");
        validateRequiredText(dto.getAxeAmeliorationEncadrantPro(), "Les axes d'amelioration de l'encadrant professionnel sont obligatoires.");

        fiche.setPointFortEncadrantPro(dto.getPointFortEncadrantPro().trim());
        fiche.setAxeAmeliorationEncadrantPro(dto.getAxeAmeliorationEncadrantPro().trim());
        fiche.setNoteFinale(fiche.calculerNoteFinale());

        return ficheEvaluationMapper.toDto(ficheEvaluationRepository.save(fiche));
    }

    @Override
    @Transactional
    public FicheEvaluationDto remplirPartieResponsableEntreprise(Long ficheId, Long userId, FicheEvaluationDto dto) {
        FicheEvaluation fiche = getFicheOrThrow(ficheId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationSectionOpen(requireStage(fiche));

        if (!isCompanyRepresentative(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul le representant de l'entreprise liee au stage peut renseigner cette partie.");
        }

        validateRequiredText(dto.getPointFortResponsableEntreprise(), "Les points forts du representant de l'entreprise sont obligatoires.");
        validateRequiredText(dto.getAxeAmeliorationResponsableEntreprise(), "Les axes d'amelioration du representant de l'entreprise sont obligatoires.");

        fiche.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise().trim());
        fiche.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise().trim());
        fiche.setNoteFinale(fiche.calculerNoteFinale());

        return ficheEvaluationMapper.toDto(ficheEvaluationRepository.save(fiche));
    }

    @Override
    @Transactional
    public FicheEvaluationDto enregistrerNotesEncadrantProfessionnel(Long ficheId, Long userId, List<NoteAttribueeDto> notes) {
        FicheEvaluation fiche = getFicheOrThrow(ficheId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationSectionOpen(requireStage(fiche));

        if (!isProfessionalSupervisor(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul l'encadrant professionnel affecte au stage peut saisir les notes.");
        }
        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Au moins une note doit etre renseignee.");
        }

        for (NoteAttribueeDto noteDto : notes) {
            upsertProfessionalNote(fiche, noteDto);
        }

        fiche.setNoteFinale(fiche.calculerNoteFinale());
        ficheEvaluationRepository.save(fiche);
        return ficheEvaluationMapper.toDto(fiche);
    }

    // ── Responsable Entreprise : enregistrement notes (Ponctualite, etc.) ─────

    @Override
    @Transactional
    public FicheEvaluationDto enregistrerNotesResponsableEntreprise(Long ficheId, Long userId, List<NoteAttribueeDto> notes) {
        FicheEvaluation fiche = getFicheOrThrow(ficheId);
        ensureEditable(fiche);
        Utilisateur utilisateur = getAuthorizedUser(userId);
        ensureEvaluationSectionOpen(requireStage(fiche));

        if (!isCompanyRepresentative(requireStage(fiche), utilisateur)) {
            throw new AccessDeniedException("Seul le representant de l'entreprise affecte au stage peut saisir ses notes.");
        }
        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Au moins une note doit etre renseignee.");
        }

        for (NoteAttribueeDto noteDto : notes) {
            upsertCompanyRepresentativeNote(fiche, noteDto);
        }

        fiche.setNoteFinale(fiche.calculerNoteFinale());
        ficheEvaluationRepository.save(fiche);
        return ficheEvaluationMapper.toDto(fiche);
    }

    private void upsertCompanyRepresentativeNote(FicheEvaluation fiche, NoteAttribueeDto dto) {
        validateNote(dto);

        CritereEvaluation critere = resolveCompanyRepresentativeCriterion(fiche, dto);
        CleNoteAttribuee id = new CleNoteAttribuee(fiche.getId(), critere.getId());

        fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee note = noteAttribueeRepository.findById(id)
                .orElseGet(() -> {
                    fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee entity = new fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee();
                    entity.setId(id);
                    entity.setFicheEvaluation(fiche);
                    entity.setCritereEvaluation(critere);
                    return entity;
                });

        note.setPoids(dto.getPoids());
        note.setBareme(dto.getBareme() != null && dto.getBareme() > 0 ? dto.getBareme() : 5);
        note.setNote(dto.getNote());
        note.setCommentaire(dto.getCommentaire() == null ? null : dto.getCommentaire().trim());
        noteAttribueeRepository.save(note);
    }

    private CritereEvaluation resolveCompanyRepresentativeCriterion(FicheEvaluation fiche, NoteAttribueeDto dto) {
        if (dto.getCritereEvaluationId() != null) {
            CritereEvaluation critere = critereEvaluationRepository.findById(dto.getCritereEvaluationId())
                    .orElseThrow(() -> new EntityNotFoundException("Critere d'evaluation introuvable."));
            if (critere.getFiche() != null && !critere.getFiche().getId().equals(fiche.getId())) {
                throw new IllegalArgumentException("Le critere d'evaluation ne correspond pas a cette fiche.");
            }
            return critere;
        }

        validateRequiredText(dto.getCritereLibelle(), "Le libelle du critere d'evaluation est obligatoire.");

        CritereEvaluation critere = new CritereEvaluation();
        critere.setLibelle(dto.getCritereLibelle().trim());
        critere.setDescription(dto.getCritereLibelle().trim());
        critere.setCategorie("Evaluation du stage");
        critere.setBareme(5);
        critere.setConsigne(null);
        critere.setPartie(PartieEvaluation.RESPONSABLE_ENTREPRISE);
        critere.setFiche(fiche);
        return critereEvaluationRepository.save(critere);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void upsertProfessionalNote(FicheEvaluation fiche, NoteAttribueeDto dto) {
        validateNote(dto);

        CritereEvaluation critere = resolveProfessionalCriterion(fiche, dto);
        CleNoteAttribuee id = new CleNoteAttribuee(fiche.getId(), critere.getId());

        fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee note = noteAttribueeRepository.findById(id)
                .orElseGet(() -> {
                    fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee entity = new fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee();
                    entity.setId(id);
                    entity.setFicheEvaluation(fiche);
                    entity.setCritereEvaluation(critere);
                    return entity;
                });

        note.setPoids(dto.getPoids());
        note.setBareme(dto.getBareme() != null && dto.getBareme() > 0 ? dto.getBareme() : 5);
        note.setNote(dto.getNote());
        note.setCommentaire(dto.getCommentaire() == null ? null : dto.getCommentaire().trim());
        noteAttribueeRepository.save(note);
    }

    private CritereEvaluation resolveProfessionalCriterion(FicheEvaluation fiche, NoteAttribueeDto dto) {
        if (dto.getCritereEvaluationId() != null) {
            CritereEvaluation critere = critereEvaluationRepository.findById(dto.getCritereEvaluationId())
                    .orElseThrow(() -> new EntityNotFoundException("Critere d'evaluation introuvable."));
            if (critere.getFiche() != null && !critere.getFiche().getId().equals(fiche.getId())) {
                throw new IllegalArgumentException("Le critere d'evaluation ne correspond pas a cette fiche.");
            }
            return critere;
        }

        validateRequiredText(dto.getCritereLibelle(), "Le libelle du critere d'evaluation est obligatoire.");

        CritereEvaluation critere = new CritereEvaluation();
        critere.setLibelle(dto.getCritereLibelle().trim());
        critere.setDescription(dto.getCritereLibelle().trim());
        critere.setCategorie("Evaluation du stage");
        critere.setBareme(5);
        critere.setConsigne(null);
        critere.setPartie(PartieEvaluation.ENCADRANT_PROFESSIONNEL);
        critere.setFiche(fiche);
        return critereEvaluationRepository.save(critere);
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

    private void ensureEvaluationSectionOpen(Stage stage) {
        boolean ouverte = stage != null
                && (Boolean.TRUE.equals(stage.getSectionEvaluationOuverte())
                || (stage.getDateFin() != null && !stage.getDateFin().isAfter(java.time.LocalDate.now())));

        if (!ouverte) {
            throw new AccessDeniedException("L'espace d'evaluation sera accessible a partir du dernier jour du stage.");
        }
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
