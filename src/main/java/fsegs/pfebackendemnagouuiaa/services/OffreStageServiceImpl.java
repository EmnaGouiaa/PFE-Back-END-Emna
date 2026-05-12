package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateOffreStageRequest;
import fsegs.pfebackendemnagouuiaa.dto.AffecterEtudiantOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.AnnulerAffectationOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.OffreStageResponse;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.OffreStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.StatutOffre;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.exception.TechnicalOperationException;
import fsegs.pfebackendemnagouuiaa.repository.AbsenceRepository;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.OffreStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableServiceStagesRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OffreStageServiceImpl implements OffreStageService {

    private static final Logger log = LoggerFactory.getLogger(OffreStageServiceImpl.class);

    private final OffreStageRepository offreStageRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final ResponsableServiceStagesRepository responsableServiceStagesRepository;
    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final ConventionStageRepository conventionStageRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final ReunionRepository reunionRepository;
    private final AbsenceRepository absenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StageService stageService;
    private final StagiaireResolutionService stagiaireResolutionService;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    public OffreStageResponse createOffre(CreateOffreStageRequest request) {
        Utilisateur authenticatedUser = getAuthenticatedUser();
        OffreStage offre = new OffreStage();

        offre.setTitre(request.getTitre());
        offre.setDescriptionMissions(request.getDescriptionMissions());
        offre.setDuree(request.getDuree());
        offre.setProfilRecherche(request.getProfilRecherche());
        offre.setDateDebutPrevue(request.getDateDebutPrevue());
        offre.setDatePublication(null);
        offre.setMotifRefus(null);

        remplirRelations(offre, request, authenticatedUser);
        applyOfferWorkflowOnCreate(offre, authenticatedUser);

        OffreStage savedOffre = offreStageRepository.save(offre);
        log.info("Offre {} creee avec statut {}", savedOffre.getId(), savedOffre.getStatut());
        return toResponse(savedOffre);
    }

    @Override
    public OffreStageResponse updateOffre(Long id, CreateOffreStageRequest request) {
        Utilisateur authenticatedUser = getAuthenticatedUser();
        OffreStage offre = findOffreById(id);
        ensureRepresentativeCanAccessOffer(offre, authenticatedUser);
        validateOfferRequest(request);
        ensureOfferCanBeUpdated(offre, authenticatedUser);

        try {
            offre.setTitre(normalizeRequiredField(request.getTitre(), "titre"));
            offre.setDescriptionMissions(normalizeRequiredField(request.getDescriptionMissions(), "descriptionMissions"));
            offre.setDuree(request.getDuree());
            offre.setProfilRecherche(normalizeOptionalText(request.getProfilRecherche()));
            offre.setDateDebutPrevue(request.getDateDebutPrevue());
            offre.setMotifRefus(null);

            applyOfferWorkflowOnUpdate(offre, authenticatedUser);
            remplirRelations(offre, request, authenticatedUser);

            return toResponse(offreStageRepository.save(offre));
        } catch (TechnicalOperationException | IllegalArgumentException | IllegalStateException
                 | AccessDeniedException | EntityNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la modification de l'offre {}", id, ex);
            throw new TechnicalOperationException(
                    "Une erreur technique est survenue lors de l'enregistrement.",
                    ex
            );
        }
    }

    @Override
    public OffreStageResponse getOffreById(Long id) {
        OffreStage offre = findOffreById(id);
        ensureRepresentativeCanAccessOffer(offre, getOptionalAuthenticatedUser());
        return toResponse(offre);
    }

    @Override
    public List<OffreStageResponse> getAllOffres() {
        Utilisateur authenticatedUser = getOptionalAuthenticatedUser();
        if (authenticatedUser != null && authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            ResponsableEntreprise responsableEntreprise = getAuthenticatedResponsableEntreprise();
            Long entrepriseId = requireEntrepriseId(responsableEntreprise);

            return offreStageRepository.findByEntrepriseId(entrepriseId)
                    .stream()
                    .filter(this::isVisibleToCompanyRepresentative)
                    .map(this::toResponse)
                    .toList();
        }

        return offreStageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<OffreStageResponse> getToutesOffresPourGestion() {
        return offreStageRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteOffre(Long id) {
        OffreStage offre = findOffreById(id);
        ensureOfferCanBeDeleted(offre);
        offreStageRepository.delete(offre);
    }

    @Override
    public OffreStageResponse publierOffre(Long offreId, Long responsableEntrepriseId) {
        OffreStage offre = findOffreById(offreId);
        ensureOffreNotAssigned(offre, "publier");
        Utilisateur authenticatedUser = getOptionalAuthenticatedUser();
        ResponsableEntreprise responsable;

        if (authenticatedUser != null && authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            responsable = getAuthenticatedResponsableEntreprise();
            if (!canManageOffer(offre, responsable)) {
                throw new AccessDeniedException("Vous ne pouvez voir que les offres de votre entreprise.");
            }
        } else {
            responsable = responsableEntrepriseRepository.findById(responsableEntrepriseId)
                    .orElseThrow(() -> new EntityNotFoundException("Responsable entreprise introuvable"));
        }

        if (offre.getStatut() == StatutOffre.EN_ATTENTE) {
            throw new IllegalStateException("L'offre doit d'abord etre validee par le responsable universitaire.");
        }

        if (offre.getStatut() == StatutOffre.REFUSEE) {
            throw new IllegalStateException("Une offre refusee ne peut pas etre publiee.");
        }

        offre.setPublieePar(responsable);
        offre.setDatePublication(LocalDate.now());
        offre.setStatut(StatutOffre.PUBLIEE);

        return toResponse(offreStageRepository.save(offre));
    }

    @Override
    public OffreStageResponse validerOffre(Long offreId, Long responsableServiceStagesId) {
        OffreStage offre = findOffreById(offreId);

        Utilisateur approver = utilisateurRepository.findById(responsableServiceStagesId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur approbateur introuvable"));

        if (!isOfferApprovalAuthorizedRole(approver.getRole())) {
            log.warn("Approbation refusee pour l'offre {}: utilisateur {} avec role {}", offreId, approver.getId(), approver.getRole());
            throw new IllegalArgumentException("Ce role n'est pas autorise a approuver les offres de stage.");
        }

        if (offre.getStatut() == null) {
            log.error("Approbation impossible pour l'offre {}: statut null", offreId);
            throw new IllegalStateException("Le statut de l'offre est invalide.");
        }

        if (offre.getStatut() != StatutOffre.EN_ATTENTE) {
            log.warn("Approbation refusee pour l'offre {}: statut actuel {}", offreId, offre.getStatut());
            throw new IllegalStateException("Seules les offres EN_ATTENTE peuvent etre approuvees.");
        }

        if (approver instanceof ResponsableServiceStages responsableServiceStages) {
            offre.setValideePar(responsableServiceStages);
        } else {
            // Some university-responsible accounts are stored as generic Utilisateur rows.
            // Approval must still succeed for role RESPONSABLE_UNIVERSITAIRE_STAGES.
            offre.setValideePar(null);
        }

        offre.setStatut(StatutOffre.VALIDEE);
        offre.setMotifRefus(null);
        offre.setDatePublication(null);

        OffreStage savedOffre = offreStageRepository.save(offre);
        log.info("Offre {} approuvee avec succes par utilisateur {} ({})", offreId, approver.getId(), approver.getRole());

        return toResponse(savedOffre, buildFullName(approver));
    }

    @Override
    public OffreStageResponse refuserOffre(Long offreId, String motifRefus) {
        OffreStage offre = findOffreById(offreId);

        if (offre.getStatut() == StatutOffre.FERMEE) {
            throw new IllegalStateException("Une offre fermee ne peut pas etre refusee.");
        }

        offre.setStatut(StatutOffre.REFUSEE);
        offre.setMotifRefus(normalizeText(motifRefus));
        offre.setDatePublication(null);
        offre.setValideePar(null);

        return toResponse(offreStageRepository.save(offre));
    }

    @Override
    public OffreStageResponse fermerOffre(Long offreId) {
        OffreStage offre = findOffreById(offreId);
        ensureRepresentativeCanAccessOffer(offre, getOptionalAuthenticatedUser());
        ensureOffreNotAssigned(offre, "fermer");
        offre.setStatut(StatutOffre.FERMEE);
        return toResponse(offreStageRepository.save(offre));
    }

    @Override
    public List<OffreStageResponse> getOffresByEntreprise(Long entrepriseId) {
        Utilisateur authenticatedUser = getOptionalAuthenticatedUser();
        if (authenticatedUser != null && authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            ResponsableEntreprise responsableEntreprise = getAuthenticatedResponsableEntreprise();
            Long authenticatedEntrepriseId = requireEntrepriseId(responsableEntreprise);

            if (entrepriseId == null || !authenticatedEntrepriseId.equals(entrepriseId)) {
                throw new AccessDeniedException("Vous ne pouvez voir que les offres de votre entreprise.");
            }
        }

        return offreStageRepository.findByEntrepriseId(entrepriseId)
                .stream()
                .filter(offre -> authenticatedUser == null
                        || authenticatedUser.getRole() != Role.RESPONSABLE_ENTREPRISE
                        || isVisibleToCompanyRepresentative(offre))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<OffreStageResponse> getOffresOuvertes() {
        List<StatutOffre> visibleStatuses = List.copyOf(EnumSet.of(StatutOffre.PUBLIEE, StatutOffre.VALIDEE));

        List<OffreStageResponse> offers = offreStageRepository.findByStatutInOrderByDatePublicationDescIdDesc(visibleStatuses)
                .stream()
                .filter(offre -> offre.getStatut() != StatutOffre.AFFECTEE)
                .filter(offre -> offre.getStatut() != StatutOffre.REFUSEE)
                .filter(offre -> !hasCreatedStage(offre))
                .map(this::toResponse)
                .toList();

        log.info("Recherche des offres ouvertes pour stagiaires -> {} offre(s) disponible(s)", offers.size());
        return offers;
    }

    @Override
    public List<OffreStageResponse> getOffresEnAttenteValidation() {
        List<OffreStageResponse> offers = offreStageRepository.findByStatutOrderByIdDesc(StatutOffre.EN_ATTENTE)
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("Recherche des offres en attente -> {} resultat(s)", offers.size());
        return offers;
    }

    @Override
    public AffecterEtudiantOffreResponse affecterEtudiant(Long offreId, String emailEtudiant) {
        String normalizedEmail = normalizeRequiredEmail(emailEtudiant);
        ResponsableEntreprise responsableAuthentifie = getAuthenticatedResponsableEntreprise();
        OffreStage offre = findOffreById(offreId);

        log.info(
                "Affectation etudiant - offreId={}, responsableId={}, email brut=[{}], email normalise=[{}], statutOffre={}",
                offreId,
                responsableAuthentifie.getId(),
                emailEtudiant,
                normalizedEmail,
                offre.getStatut()
        );

        if (!canManageOffer(offre, responsableAuthentifie)) {
            log.warn(
                    "Affectation etudiant refusee - offre {} hors perimetre du responsable {} (email={}, entrepriseId={})",
                    offreId,
                    responsableAuthentifie.getId(),
                    responsableAuthentifie.getEmail(),
                    responsableAuthentifie.getEntreprise() != null ? responsableAuthentifie.getEntreprise().getId() : null
            );
            throw new AccessDeniedException("Vous ne pouvez affecter un étudiant qu’à une offre de votre entreprise.");
        }

        if (hasCreatedStage(offre) || offre.getStatut() == StatutOffre.AFFECTEE) {
            log.warn("Affectation etudiant refusee - offre {} deja affectee ou stage deja cree", offreId);
            throw new IllegalStateException("Cette offre est deja affectee a un etudiant.");
        }

        if (offre.getStatut() != StatutOffre.VALIDEE) {
            log.warn("Affectation etudiant refusee - offre {} non validee ({})", offreId, offre.getStatut());
            throw new IllegalStateException("Offre non validee.");
        }

        if (offre.getStatut() == StatutOffre.REFUSEE) {
            throw new IllegalStateException("Offre refusée non disponible.");
        }

        Stagiaire stagiaire = stagiaireResolutionService.findByEmail(normalizedEmail);
        log.info("Affectation etudiant - stagiaire resolu: id={}, email={}", stagiaire.getId(), stagiaire.getEmail());
        existsStageActifByStagiaire(stagiaire, offre);

        offre.setStagiaireAffecte(stagiaire);
        offre.setStatut(StatutOffre.AFFECTEE);
        offreStageRepository.save(offre);
        notificationService.notifierStageAffecte(
                stagiaire.getId(),
                null,
                offre.getTitre(),
                offre.getEntreprise() != null ? offre.getEntreprise().getNom() : null
        );

        log.info("Offre {} affectee au stagiaire {} sans declenchement du stage", offreId, stagiaire.getId());

        return AffecterEtudiantOffreResponse.builder()
                .message("Etudiant affecte a l'offre avec succes.")
                .offreId(offre.getId())
                .offreStatut(offre.getStatut().name())
                .stageId(null)
                .stageTitre(null)
                .stagiaireId(stagiaire.getId())
                .stagiaireEmail(stagiaire.getEmail())
                .stageDeclenche(false)
                .trelloEnabled(false)
                .build();
    }

    @Override
    @Transactional
    public AnnulerAffectationOffreResponse annulerAffectation(Long offreId) {
        ResponsableEntreprise responsableAuthentifie = getAuthenticatedResponsableEntreprise();
        OffreStage offre = findOffreById(offreId);

        if (!canManageOffer(offre, responsableAuthentifie)) {
            log.warn(
                    "Annulation affectation refusee - offre {} hors perimetre du responsable {} (email={}, entrepriseId={})",
                    offreId,
                    responsableAuthentifie.getId(),
                    responsableAuthentifie.getEmail(),
                    responsableAuthentifie.getEntreprise() != null ? responsableAuthentifie.getEntreprise().getId() : null
            );
            throw new AccessDeniedException("Action non autorisee pour cette offre.");
        }

        Stagiaire stagiaireAffecte = offre.getStagiaireAffecte();
        if (stagiaireAffecte == null || stagiaireAffecte.getId() == null) {
            log.warn("Annulation affectation impossible - aucune affectation active trouvee pour l'offre {}", offreId);
            throw new IllegalStateException("Aucune affectation active trouvee pour cette offre.");
        }

        Long stagiaireId = stagiaireAffecte.getId();
        Stage linkedStage = findLatestStageForOffer(offreId);
        if (linkedStage != null) {
            if (linkedStage.getStatut() != StatutStage.PAS_COMMENCE) {
                throw new IllegalStateException("Impossible d\u2019annuler l\u2019affectation : le stage est d\u00e9j\u00e0 d\u00e9clench\u00e9.");
            }
            linkedStage.setStatut(StatutStage.ANNULE);
            stageRepository.save(linkedStage);
        }

        offre.setStagiaireAffecte(null);
        offre.setStatut(StatutOffre.VALIDEE);
        offre.setMotifRefus(null);
        offreStageRepository.save(offre);

        log.info("Affectation stagiaire/offre annulee avant declenchement du stage. offreId={}, stagiaireId={}", offreId, stagiaireId);

        return AnnulerAffectationOffreResponse.builder()
                .message("Affectation annulee avec succes.")
                .offreId(offre.getId())
                .offreStatut(offre.getStatut().name())
                .stageId(linkedStage != null ? linkedStage.getId() : null)
                .stageStatut(linkedStage != null && linkedStage.getStatut() != null ? linkedStage.getStatut().name() : null)
                .modeAnnulation(linkedStage != null ? "ANNULATION_STAGE" : "ANNULATION_AFFECTATION")
                .build();
    }

    private void remplirRelations(OffreStage offre, CreateOffreStageRequest request, Utilisateur authenticatedUser) {
        Entreprise entreprise = resolveEntrepriseForOfferRequest(request, authenticatedUser);
        offre.setEntreprise(entreprise);

        if (authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE && request.getPublieeParId() != null) {
            ResponsableEntreprise responsableEntreprise = responsableEntrepriseRepository.findById(request.getPublieeParId())
                    .orElseThrow(() -> new EntityNotFoundException("Responsable entreprise introuvable"));
            offre.setPublieePar(responsableEntreprise);
        } else {
            offre.setPublieePar(null);
        }

        if (!isDirectOfferManagementRole(authenticatedUser) && request.getValideeParId() != null) {
            ResponsableServiceStages responsableServiceStages = responsableServiceStagesRepository.findById(request.getValideeParId())
                    .orElseThrow(() -> new EntityNotFoundException("Responsable service stages introuvable"));
            offre.setValideePar(responsableServiceStages);
        } else if (!isDirectOfferManagementRole(authenticatedUser)) {
            offre.setValideePar(null);
        }
    }

    private void applyOfferWorkflowOnCreate(OffreStage offre, Utilisateur authenticatedUser) {
        if (isDirectOfferManagementRole(authenticatedUser)) {
            offre.setStatut(StatutOffre.VALIDEE);
            assignValidatedBy(offre, authenticatedUser);
            return;
        }

        offre.setStatut(StatutOffre.EN_ATTENTE);
        offre.setValideePar(null);
    }

    private void applyOfferWorkflowOnUpdate(OffreStage offre, Utilisateur authenticatedUser) {
        if (isDirectOfferManagementRole(authenticatedUser)) {
            offre.setStatut(StatutOffre.VALIDEE);
            offre.setDatePublication(null);
            offre.setMotifRefus(null);
            assignValidatedBy(offre, authenticatedUser);
            return;
        }

        if (offre.getStatut() == StatutOffre.VALIDEE
                || offre.getStatut() == StatutOffre.PUBLIEE
                || offre.getStatut() == StatutOffre.REFUSEE) {
            offre.setStatut(StatutOffre.EN_ATTENTE);
            offre.setValideePar(null);
            offre.setDatePublication(null);
        }
    }

    private void assignValidatedBy(OffreStage offre, Utilisateur authenticatedUser) {
        if (authenticatedUser instanceof ResponsableServiceStages responsableServiceStages) {
            offre.setValideePar(responsableServiceStages);
            return;
        }

        offre.setValideePar(null);
    }

    private boolean isDirectOfferManagementRole(Utilisateur authenticatedUser) {
        return authenticatedUser.getRole() == Role.RESPONSABLE_UNIVERSITAIRE_STAGES
                || authenticatedUser.getRole() == Role.RESPONSABLE_SERVICE_STAGES
                || authenticatedUser.getRole() == Role.ADMINISTRATEUR;
    }

    private Utilisateur getAuthenticatedUser() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifie introuvable."));
    }

    private Utilisateur getOptionalAuthenticatedUser() {
        return jwtService.getAuthenticatedUtilisateur().orElse(null);
    }

    private OffreStage findOffreById(Long id) {
        return offreStageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre de stage introuvable."));
    }

    private OffreStageResponse toResponse(OffreStage offre) {
        return toResponse(offre, buildFullName(offre.getValideePar()));
    }

    private OffreStageResponse toResponse(OffreStage offre, String approvedByName) {
        Stage linkedStage = findLatestStageForOffer(offre.getId());
        boolean stageCree = linkedStage != null;
        boolean stageDeclenche = isStageDeclenche(linkedStage);
        return OffreStageResponse.builder()
                .id(offre.getId())
                .titre(offre.getTitre())
                .descriptionMissions(offre.getDescriptionMissions())
                .duree(offre.getDuree())
                .profilRecherche(offre.getProfilRecherche())
                .dateDebutPrevue(offre.getDateDebutPrevue())
                .datePublication(offre.getDatePublication())
                .statut(offre.getStatut() != null ? offre.getStatut().name() : null)
                .motifRefus(offre.getMotifRefus())
                .entrepriseId(offre.getEntreprise() != null ? offre.getEntreprise().getId() : null)
                .entrepriseNom(offre.getEntreprise() != null ? offre.getEntreprise().getNom() : null)
                .publieeParId(offre.getPublieePar() != null ? offre.getPublieePar().getId() : null)
                .publieeParNomComplet(buildFullName(offre.getPublieePar()))
                .valideeParId(offre.getValideePar() != null ? offre.getValideePar().getId() : null)
                .valideeParNomComplet(approvedByName)
                .stageCree(stageCree)
                .stageDeclenche(stageDeclenche)
                .trelloEnabled(linkedStage != null && linkedStage.getStatut() == StatutStage.EN_COURS)
                .affectable(isOfferAssignable(offre, hasCreatedStage(offre)))
                .build();
    }

    private String buildFullName(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }

        return (utilisateur.getPrenom() + " " + utilisateur.getNom()).trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalText(String value) {
        return normalizeText(value);
    }

    private String normalizeRequiredField(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Tous les champs obligatoires doivent être renseignés.");
        }
        return normalized;
    }

    private boolean isOfferApprovalAuthorizedRole(Role role) {
        return role == Role.RESPONSABLE_SERVICE_STAGES
                || role == Role.RESPONSABLE_UNIVERSITAIRE_STAGES
                || role == Role.ADMINISTRATEUR;
    }

    private ResponsableEntreprise getAuthenticatedResponsableEntreprise() {
        Utilisateur authenticatedUser = getAuthenticatedUser();

        if (authenticatedUser.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Seul le representant de l'entreprise peut affecter un etudiant.");
        }

        if (authenticatedUser instanceof ResponsableEntreprise responsableEntreprise) {
            return responsableEntreprise;
        }

        return responsableEntrepriseRepository.findById(authenticatedUser.getId())
                .or(() -> responsableEntrepriseRepository.findByEmailIgnoreCase(authenticatedUser.getEmail()))
                .orElseThrow(() -> new AccessDeniedException("Representant entreprise introuvable pour le compte authentifie."));
    }

    private String normalizeRequiredEmail(String email) {
        String normalized = normalizeText(email);
        if (normalized == null) {
            throw new IllegalArgumentException("L'email de l'etudiant est obligatoire.");
        }
        return normalized;
    }

    private Entreprise resolveEntrepriseForOfferRequest(CreateOffreStageRequest request, Utilisateur authenticatedUser) {
        if (authenticatedUser != null && authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            ResponsableEntreprise responsableEntreprise = getAuthenticatedResponsableEntreprise();
            Entreprise entreprise = responsableEntreprise.getEntreprise();
            if (entreprise == null || entreprise.getId() == null) {
                throw new IllegalStateException("Aucune entreprise n'est rattachée au représentant connecté.");
            }
            return entreprise;
        }

        if (request.getEntrepriseId() == null) {
            throw new EntityNotFoundException("Entreprise introuvable");
        }

        return entrepriseRepository.findById(request.getEntrepriseId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable"));
    }

    private void ensureOffreNotAssigned(OffreStage offre, String action) {
        if (offre.getStatut() == StatutOffre.AFFECTEE || hasCreatedStage(offre)) {
            throw new IllegalStateException("Cette offre est deja affectee et ne peut plus etre " + action + ".");
        }
    }

    private void ensureOfferCanBeUpdated(OffreStage offre, Utilisateur authenticatedUser) {
        ensureOffreNotAssigned(offre, "modifiee");

        if (authenticatedUser == null || authenticatedUser.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            return;
        }

        if (offre.getStatut() == StatutOffre.FERMEE) {
            throw new IllegalStateException("Cette offre n'est pas modifiable selon son statut actuel.");
        }

        if (offre.getStatut() == null) {
            throw new IllegalStateException("Cette offre n'est pas modifiable selon son statut actuel.");
        }
    }

    private void validateOfferRequest(CreateOffreStageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Tous les champs obligatoires doivent être renseignés.");
        }

        if (normalizeText(request.getTitre()) == null
                || normalizeText(request.getDescriptionMissions()) == null
                || request.getDateDebutPrevue() == null) {
            throw new IllegalArgumentException("Tous les champs obligatoires doivent être renseignés.");
        }

        if (normalizeText(request.getTitre()).length() < 3
                || normalizeText(request.getDescriptionMissions()).length() < 10) {
            throw new IllegalArgumentException("Les données saisies sont invalides.");
        }

        Integer duree = request.getDuree();
        if (duree != null && duree <= 0) {
            throw new IllegalArgumentException("Les données saisies sont invalides.");
        }

        if (request.getDateDebutPrevue().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Les données saisies sont invalides.");
        }

        String profilRecherche = normalizeText(request.getProfilRecherche());
        if (profilRecherche != null && profilRecherche.length() < 2) {
            throw new IllegalArgumentException("Les données saisies sont invalides.");
        }
    }

    private void ensureOfferCanBeDeleted(OffreStage offre) {
        if (hasCreatedStage(offre) || offre.getStatut() == StatutOffre.AFFECTEE) {
            throw new IllegalStateException("Cette offre est deja affectee ou liee a un stage et ne peut pas etre supprimee.");
        }
    }

    private void ensureRepresentativeCanAccessOffer(OffreStage offre, Utilisateur authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            return;
        }

        ResponsableEntreprise responsableEntreprise = getAuthenticatedResponsableEntreprise();
        if (!canManageOffer(offre, responsableEntreprise)) {
            throw new AccessDeniedException("Action non autorisée sur cette offre.");
        }
    }

    private boolean hasCreatedStage(OffreStage offre) {
        if (offre.getId() == null) {
            return false;
        }

        Stage linkedStage = findLatestStageForOffer(offre.getId());
        return linkedStage != null && linkedStage.getStatut() != StatutStage.ANNULE;
    }

    private boolean isOfferAssignable(OffreStage offre, boolean stageCree) {
        return !stageCree && offre.getStatut() == StatutOffre.VALIDEE;
    }

    private void existsStageActifByStagiaire(Stagiaire stagiaire, OffreStage offre) {
        LocalDate requestedStart = offre.getDateDebutPrevue();
        LocalDate requestedEnd = calculateOfferEndDate(offre);

        if (requestedStart == null) {
            return;
        }

        boolean conflict = stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(stage -> stage.getStatut() != null)
                .filter(stage -> stage.getStatut() != StatutStage.REFUSE)
                .filter(stage -> stage.getStatut() != StatutStage.ANNULE)
                .anyMatch(stage -> periodsOverlap(
                        requestedStart,
                        requestedEnd,
                        stage.getDateDebut(),
                        calculateStageEndDate(stage)
                ));

        if (conflict) {
            log.warn("Affectation etudiant refusee - conflit de periode detecte pour stagiaire {}", stagiaire.getId());
            throw new IllegalStateException("Cet etudiant est deja affecte a un stage sur cette periode.");
        }
    }

    private LocalDate calculateOfferEndDate(OffreStage offre) {
        if (offre.getDateDebutPrevue() == null) {
            return null;
        }

        if (offre.getDuree() == null || offre.getDuree() <= 0) {
            return offre.getDateDebutPrevue();
        }

        return offre.getDateDebutPrevue().plusMonths(offre.getDuree().longValue());
    }

    private LocalDate calculateStageEndDate(Stage stage) {
        if (stage.getDateFin() != null) {
            return stage.getDateFin();
        }

        if (stage.getDateDebut() == null) {
            return null;
        }

        if (stage.getDuree() != null && stage.getDuree() > 0) {
            return stage.getDateDebut().plusMonths(stage.getDuree().longValue());
        }

        return stage.getDateDebut();
    }

    private boolean periodsOverlap(LocalDate requestedStart, LocalDate requestedEnd, LocalDate existingStart, LocalDate existingEnd) {
        if (requestedStart == null || existingStart == null) {
            return false;
        }

        LocalDate normalizedRequestedEnd = requestedEnd != null ? requestedEnd : requestedStart;
        LocalDate normalizedExistingEnd = existingEnd != null ? existingEnd : existingStart;

        return !normalizedExistingEnd.isBefore(requestedStart)
                && !normalizedRequestedEnd.isBefore(existingStart);
    }

    private boolean canManageOffer(OffreStage offre, ResponsableEntreprise responsable) {
        if (offre == null || responsable == null) {
            return false;
        }

        Long offerEntrepriseId = offre.getEntreprise() != null ? offre.getEntreprise().getId() : null;
        Long responsableEntrepriseId = requireEntrepriseId(responsable);
        return offerEntrepriseId != null && offerEntrepriseId.equals(responsableEntrepriseId);
    }

    private Stage findTriggeredStage(Long offreId, Long stagiaireId) {
        if (offreId == null || stagiaireId == null) {
            return null;
        }

        return stageRepository.findFirstByStagiaireIdAndOffreSourceId(stagiaireId, offreId)
                .orElse(null);
    }

    private Stage findLatestStageForOffer(Long offreId) {
        if (offreId == null) {
            return null;
        }

        return stageRepository.findFirstByOffreSourceIdOrderByIdDesc(offreId)
                .orElse(null);
    }

    private boolean isStageDeclenche(Stage stage) {
        return stage != null
                && stage.getStatut() != null
                && stage.getStatut() != StatutStage.PAS_COMMENCE
                && stage.getStatut() != StatutStage.ANNULE
                && stage.getStatut() != StatutStage.REFUSE;
    }

    private boolean isVisibleToCompanyRepresentative(OffreStage offre) {
        return offre != null && offre.getStatut() != StatutOffre.REFUSEE;
    }

    private Long requireEntrepriseId(ResponsableEntreprise responsable) {
        Long entrepriseId = responsable != null && responsable.getEntreprise() != null
                ? responsable.getEntreprise().getId()
                : null;
        if (entrepriseId == null) {
            throw new IllegalStateException("Aucune entreprise n'est rattachée au représentant connecté.");
        }
        return entrepriseId;
    }

    private boolean canDeleteStagePhysically(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return false;
        }

        return !hasStageFollowUpData(stage)
                && !hasStageSignatures(stage)
                && !hasTrelloTrace(stage);
    }

    private boolean hasStageFollowUpData(Stage stage) {
        Long stageId = stage.getId();
        if (stageId == null) {
            return false;
        }

        if (stage.getDemandeStage() != null) {
            return true;
        }

        return cahierStageRepository.existsByStageId(stageId)
                || conventionStageRepository.existsByStageId(stageId)
                || ficheEvaluationRepository.existsByStageId(stageId)
                || !reunionRepository.findByStageId(stageId).isEmpty()
                || !absenceRepository.findByStageId(stageId).isEmpty();
    }

    private boolean hasStageSignatures(Stage stage) {
        Long stageId = stage.getId();
        if (stageId == null) {
            return false;
        }

        boolean conventionSignee = conventionStageRepository.findByStageId(stageId)
                .map(convention -> Boolean.TRUE.equals(convention.getSigneeEncAca())
                        || Boolean.TRUE.equals(convention.getSigneeEncPro())
                        || Boolean.TRUE.equals(convention.getSigneeEntreprise())
                        || Boolean.TRUE.equals(convention.getSigneeResp())
                        || Boolean.TRUE.equals(convention.getSigneeStagiaire()))
                .orElse(false);

        boolean cahierSigne = cahierStageRepository.findByStageId(stageId)
                .map(cahier -> Boolean.TRUE.equals(cahier.getEstSigne())
                        || Boolean.TRUE.equals(cahier.getSigneeEncAcad())
                        || Boolean.TRUE.equals(cahier.getSigneeEncPro())
                        || Boolean.TRUE.equals(cahier.getSigneeRespEntreprise())
                        || Boolean.TRUE.equals(cahier.getSigneeStagiaire()))
                .orElse(false);

        boolean ficheSignee = ficheEvaluationRepository.findFirstByStageId(stageId)
                .map(fiche -> (fiche.getSignatureEncadrantProfessionnel() != null && !fiche.getSignatureEncadrantProfessionnel().isBlank())
                        || (fiche.getSignatureRepresentantEntreprise() != null && !fiche.getSignatureRepresentantEntreprise().isBlank()))
                .orElse(false);

        return conventionSignee || cahierSigne || ficheSignee;
    }

    private boolean hasTrelloTrace(Stage stage) {
        return normalizeText(stage.getTrelloBoardId()) != null
                || normalizeText(stage.getTrelloBoardUrl()) != null
                || normalizeText(stage.getTrelloTodoListId()) != null
                || normalizeText(stage.getTrelloDoingListId()) != null
                || normalizeText(stage.getTrelloDoneListId()) != null;
    }

    private String buildStageCancellationReasons(Stage stage) {
        Long stageId = stage.getId();
        if (stageId == null) {
            return "stage sans identifiant";
        }

        StringBuilder reasons = new StringBuilder();
        if (stage.getDemandeStage() != null) {
            reasons.append("demande-stage;");
        }
        if (cahierStageRepository.existsByStageId(stageId)) {
            reasons.append("cahier-stage;");
        }
        if (conventionStageRepository.existsByStageId(stageId)) {
            reasons.append("convention;");
        }
        if (ficheEvaluationRepository.existsByStageId(stageId)) {
            reasons.append("fiche-evaluation;");
        }
        if (!reunionRepository.findByStageId(stageId).isEmpty()) {
            reasons.append("reunions;");
        }
        if (!absenceRepository.findByStageId(stageId).isEmpty()) {
            reasons.append("absences;");
        }
        if (hasStageSignatures(stage)) {
            reasons.append("signatures;");
        }
        if (hasTrelloTrace(stage)) {
            reasons.append("trello;");
        }

        return reasons.length() > 0 ? reasons.toString() : "aucune";
    }
}
