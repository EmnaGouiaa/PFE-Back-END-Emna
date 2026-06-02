package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateOffreStageRequest;
import fsegs.pfebackendemnagouuiaa.validation.StagePeriodValidation;
import fsegs.pfebackendemnagouuiaa.dto.AffecterEtudiantOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.AnnulerAffectationOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.OffreStageResponse;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.OffreStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
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
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private final EncadrantProfessionnelRepository encadrantProfessionnelRepository;
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

        // Validation des donnees (champs obligatoires, duree 1-4 mois, date de debut >= aujourd'hui...).
        // Appliquee a la creation comme a la modification pour une regle coherente.
        validateOfferRequest(request);

        // Période de création : uniquement pour le rôle RESPONSABLE_ENTREPRISE.
        // Les rôles RESPONSABLE_STAGE et ADMINISTRATEUR ne sont pas soumis à cette restriction.
        if (authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            validateCreationPeriod();
        }

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
        ensureOfferNotLockedByTerminatedStage(offre);

        // Regle metier (RESPONSABLE_ENTREPRISE uniquement) :
        //   - REFUSEE                                : modification interdite (bloquage definitif).
        //   - EN_ATTENTE (avant approbation)          : modification libre integrale.
        //   - VALIDEE / PUBLIEE / AFFECTEE (approuvee): seuls les champs non critiques (titre,
        //     profil recherche, encadrant pro) peuvent etre modifies. Description, duree et
        //     date de debut sont silencieusement preservees pour ne pas contourner la validation
        //     responsable stages / academique.
        //   - TERMINEE / ARCHIVEE / FERMEE            : pris en charge par ensureOfferCanBeUpdated.
        if (authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            if (offre.getStatut() == StatutOffre.REFUSEE) {
                throw new IllegalStateException(
                        "Cette offre a été refusée par le Responsable des Stages et ne peut plus être modifiée. "
                                + "Veuillez créer une nouvelle offre pour toute correction.");
            }
            // Nouveau verrou metier :
            // apres affectation, le representant entreprise peut uniquement modifier
            // description + date de debut tant que le sujet est EN_ATTENTE de validation academique.
            if (offre.getStatut() == StatutOffre.AFFECTEE) {
                return updateAffecteeOfferWhileSubjectPending(offre, request, authenticatedUser);
            }
            if (offre.getStatut() != StatutOffre.EN_ATTENTE) {
                return updateNonCriticalFieldsOnly(offre, request, authenticatedUser);
            }
        }

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

    private OffreStageResponse updateAffecteeOfferWhileSubjectPending(
            OffreStage offre,
            CreateOffreStageRequest request,
            Utilisateur authenticatedUser
    ) {
        Stage linkedStage = findLatestStageForOffer(offre.getId());
        if (linkedStage == null || linkedStage.getStatut() == StatutStage.ANNULE) {
            throw new IllegalStateException("Aucun stage actif n'est lie a cette offre affectee.");
        }
        if (linkedStage.getStatutSujet() == StatutValidation.VALIDEE) {
            throw new IllegalStateException(
                    "Modification interdite : le sujet a deja ete valide par l'encadrant academique."
            );
        }

        String previousDescription = offre.getDescriptionMissions();
        LocalDate previousDateDebut = offre.getDateDebutPrevue();

        String nouvelleDescription = normalizeRequiredField(request.getDescriptionMissions(), "descriptionMissions");
        if (nouvelleDescription.length() < 10) {
            throw new IllegalArgumentException("La description des missions doit contenir au moins 10 caracteres.");
        }
        if (request.getDateDebutPrevue() == null) {
            throw new IllegalArgumentException("La date de debut du stage est obligatoire.");
        }
        if (request.getDateDebutPrevue().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La date de debut du stage ne peut pas etre dans le passe. Veuillez choisir une date actuelle ou future."
            );
        }

        offre.setDescriptionMissions(nouvelleDescription);
        offre.setDateDebutPrevue(request.getDateDebutPrevue());
        OffreStage saved = offreStageRepository.save(offre);

        logOfferControlledUpdateHistory(saved, previousDescription, previousDateDebut);
        notifyActorsAfterOfferUpdate(saved, linkedStage, authenticatedUser);
        return toResponse(saved);
    }

    /**
     * Mise a jour apres validation academique : seuls les champs non sensibles peuvent etre
     * mis a jour. Sensibles (verrouilles) : description des missions, duree, date de debut.
     * Non sensibles (modifiables) : titre, profil recherche, encadrant professionnel.
     * Le statut de l'offre n'est pas reinitialise (pas de retour a EN_ATTENTE), pour ne pas
     * perturber le suivi en cours.
     */
    private OffreStageResponse updateNonCriticalFieldsOnly(
            OffreStage offre,
            CreateOffreStageRequest request,
            Utilisateur authenticatedUser
    ) {
        try {
            offre.setTitre(normalizeRequiredField(request.getTitre(), "titre"));
            if (offre.getTitre().length() < 3) {
                throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères.");
            }
            offre.setProfilRecherche(normalizeOptionalText(request.getProfilRecherche()));
            // Mise a jour de l'encadrant pro (champ non sensible cote academique).
            // assignEncadrantProToOffre verifie l'appartenance a l'entreprise et l'obligation.
            assignEncadrantProToOffre(offre, request, offre.getEntreprise(), authenticatedUser);
            // descriptionMissions, duree, dateDebutPrevue : valeurs en base preservees.
            return toResponse(offreStageRepository.save(offre));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la mise a jour partielle (post-validation) de l'offre {}", offre.getId(), ex);
            throw new TechnicalOperationException(
                    "Une erreur technique est survenue lors de l'enregistrement.",
                    ex
            );
        }
    }

    /**
     * @deprecated Conserve pour reference. Remplace par {@link #updateNonCriticalFieldsOnly}
     * suite a la nouvelle regle metier (verrou partiel apres validation au lieu de blocage total).
     */
    @Deprecated
    @SuppressWarnings("unused")
    private OffreStageResponse updateDescriptionOnly(OffreStage offre, CreateOffreStageRequest request) {
        try {
            String desc = normalizeRequiredField(request.getDescriptionMissions(), "descriptionMissions");
            if (desc.length() < 10) {
                throw new IllegalArgumentException(
                        "La description des missions doit contenir au moins 10 caractères.");
            }
            offre.setDescriptionMissions(desc);
            return toResponse(offreStageRepository.save(offre));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Erreur technique lors de la mise a jour partielle (description) de l'offre {}", offre.getId(), ex);
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
        ensureOfferNotLockedByTerminatedStage(offre);
        ensureOfferCanBeDeleted(offre);
        offreStageRepository.delete(offre);
    }

    @Override
    public OffreStageResponse publierOffre(Long offreId, Long responsableEntrepriseId) {
        OffreStage offre = findOffreById(offreId);
        ensureOfferNotLockedByTerminatedStage(offre);
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
        ensureOfferNotLockedByTerminatedStage(offre);

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
            // Approval must still succeed for role RESPONSABLE_STAGE.
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
        ensureOfferNotLockedByTerminatedStage(offre);

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
        ensureOfferNotLockedByTerminatedStage(offre);
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
        // Garde-fou : on archive a la volee les offres expirees non affectees, pour qu'elles
        // disparaissent immediatement de la vue stagiaire sans attendre le passage du cron.
        archiveExpiredUnassignedOffers();

        List<StatutOffre> visibleStatuses = List.copyOf(EnumSet.of(StatutOffre.PUBLIEE, StatutOffre.VALIDEE));
        LocalDate today = LocalDate.now();

        List<OffreStageResponse> offers = offreStageRepository.findByStatutInOrderByDatePublicationDescIdDesc(visibleStatuses)
                .stream()
                .filter(offre -> offre.getStatut() != StatutOffre.AFFECTEE)
                .filter(offre -> offre.getStatut() != StatutOffre.REFUSEE)
                .filter(offre -> offre.getStatut() != StatutOffre.ARCHIVEE)
                .filter(offre -> offre.getStatut() != StatutOffre.TERMINEE)
                // Spec : un stagiaire ne doit jamais voir une offre dont la date de debut est depassee.
                .filter(offre -> offre.getDateDebutPrevue() == null || !offre.getDateDebutPrevue().isBefore(today))
                .filter(offre -> !hasCreatedStage(offre))
                .map(this::toResponse)
                .toList();

        log.info("Recherche des offres ouvertes pour stagiaires -> {} offre(s) disponible(s)", offers.size());
        return offers;
    }

    /**
     * Tache planifiee : pose le statut final TERMINEE sur les offres en fin de cycle de vie.
     *
     * Couvre deux cas (cycle de vie unifie autour de l'etat final TERMINEE) :
     *  1) Offres dont la date de debut est passee SANS qu'un etudiant ne soit affecte
     *     (anciennement ARCHIVEE).
     *  2) Offres affectees a un etudiant DONT le sujet a ete valide par l'encadrant
     *     academique ET dont la date de fin de stage est atteinte.
     *
     * Cron par defaut : tous les jours a 02:00 (Africa/Tunis), parametrable via app.offre.archivage.cron.
     */
    @Scheduled(
            cron = "${app.offre.archivage.cron:0 0 2 * * *}",
            zone = "${app.offre.archivage.zone:Africa/Tunis}"
    )
    @Transactional
    public int archiveExpiredUnassignedOffers() {
        LocalDate today = LocalDate.now();

        int terminated = 0;

        // ─── Cas 1 : offres expirees non affectees → TERMINEE ─────────────────────────
        List<StatutOffre> finalizableStatuses = List.of(
                StatutOffre.EN_ATTENTE,
                StatutOffre.VALIDEE,
                StatutOffre.PUBLIEE,
                StatutOffre.REFUSEE
        );
        List<OffreStage> expiredUnassigned = offreStageRepository.findAll().stream()
                .filter(offre -> offre.getDateDebutPrevue() != null && offre.getDateDebutPrevue().isBefore(today))
                .filter(offre -> offre.getStagiaireAffecte() == null)
                .filter(offre -> finalizableStatuses.contains(offre.getStatut()))
                .filter(offre -> !hasCreatedStage(offre))
                .toList();
        for (OffreStage offre : expiredUnassigned) {
            offre.setStatut(StatutOffre.TERMINEE);
            offreStageRepository.save(offre);
            terminated++;
            log.info("Offre {} marquee TERMINEE automatiquement (date debut {} passee, sans affectation).",
                    offre.getId(), offre.getDateDebutPrevue());
        }

        // ─── Cas 2 : offres affectees + sujet valide + stage fini → TERMINEE ──────────
        List<OffreStage> assignedFinished = offreStageRepository.findAll().stream()
                .filter(offre -> offre.getStatut() == StatutOffre.AFFECTEE)
                .filter(offre -> {
                    Stage stage = findLatestStageForOffer(offre.getId());
                    if (stage == null || stage.getStatut() == StatutStage.ANNULE) return false;
                    if (stage.getStatutSujet() != StatutValidation.VALIDEE) return false;
                    return stage.getDateFin() != null && !stage.getDateFin().isAfter(today);
                })
                .toList();
        for (OffreStage offre : assignedFinished) {
            offre.setStatut(StatutOffre.TERMINEE);
            offreStageRepository.save(offre);
            terminated++;
            log.info("Offre {} marquee TERMINEE automatiquement (stage termine, sujet valide).", offre.getId());
        }

        if (terminated > 0) {
            log.info("Transition automatique : {} offre(s) passees a TERMINEE.", terminated);
        }
        return terminated;
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
    @Transactional
    public AffecterEtudiantOffreResponse affecterEtudiant(Long offreId, String emailEtudiant) {
        String normalizedEmail = normalizeRequiredEmail(emailEtudiant);
        ResponsableEntreprise responsableAuthentifie = getAuthenticatedResponsableEntreprise();
        OffreStage offre = findOffreById(offreId);
        ensureOfferNotLockedByTerminatedStage(offre);

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
            throw new AccessDeniedException("Vous ne pouvez affecter un étudiant quéé une offre de votre entreprise.");
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
        ensureNoExistingOfferAssignment(stagiaire, offre);
        existsStageActifByStagiaire(stagiaire, offre);

        offre.setStagiaireAffecte(stagiaire);
        offre.setStatut(StatutOffre.AFFECTEE);
        offreStageRepository.save(offre);

        Stage savedStage = stageService.creerStageDepuisOffrePourEntreprise(offre, stagiaire, responsableAuthentifie);
        log.info("Stage cree via StageService: id={}, titre={}, stagiaireId={}, statut={}",
                savedStage.getId(), savedStage.getTitre(),
                savedStage.getStagiaire() != null ? savedStage.getStagiaire().getId() : null,
                savedStage.getStatut());

        try {
            notificationService.notifierStageAffecte(
                    stagiaire.getId(),
                    savedStage.getId(),
                    offre.getTitre(),
                    offre.getEntreprise() != null ? offre.getEntreprise().getNom() : null
            );
            log.info("Notification de stage affecte creee pour stagiaireId={}, stageId={}", stagiaire.getId(), savedStage.getId());
        } catch (Exception ex) {
            log.warn("Erreur lors de la creation de la notification de stage affecte: {}", ex.getMessage(), ex);
        }

        log.info("Offre {} affectee au stagiaire {} avec creation du stage {}", offreId, stagiaire.getId(), savedStage.getId());

        return AffecterEtudiantOffreResponse.builder()
                .message("Etudiant affecte a l'offre avec succes.")
                .offreId(offre.getId())
                .offreStatut(offre.getStatut().name())
                .stageId(savedStage.getId())
                .stageTitre(savedStage.getTitre())
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
        ensureOfferNotLockedByTerminatedStage(offre);

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
        Long deletedStageId = null;
        if (linkedStage != null) {
            if (!isAnnulationAffectationAutorisee(offre, linkedStage)) {
                throw new IllegalStateException(buildAnnulationAffectationRefusalMessage(linkedStage));
            }
            // Regle metier : suppression complete du stage afin qu'il ne soit plus visible
            // pour aucun acteur (etudiant, encadrants, agent, admin, responsable entreprise).
            // Les entites liees (convention, cahier, fiche d'evaluation, reunions, notifications,
            // absences) sont supprimees en cascade via les annotations
            // @OneToOne/@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true) declarees dans Stage.
            deletedStageId = linkedStage.getId();
            stageRepository.delete(linkedStage);
            stageRepository.flush();
        }

        offre.setStagiaireAffecte(null);
        offre.setStatut(StatutOffre.VALIDEE);
        offre.setMotifRefus(null);
        offreStageRepository.save(offre);

        log.info("Affectation annulee avec suppression du stage. offreId={}, stagiaireId={}, stageSupprimeId={}", offreId, stagiaireId, deletedStageId);

        String message = deletedStageId != null
                ? "L\u2019affectation a \u00e9t\u00e9 annul\u00e9e. Le stage associ\u00e9 a \u00e9t\u00e9 supprim\u00e9 automatiquement."
                : "Affectation annul\u00e9e avec succ\u00e8s.";

        return AnnulerAffectationOffreResponse.builder()
                .message(message)
                .offreId(offre.getId())
                .offreStatut(offre.getStatut().name())
                .stageId(deletedStageId)
                .stageStatut(null)
                .modeAnnulation(deletedStageId != null ? "SUPPRESSION_STAGE" : "ANNULATION_AFFECTATION")
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

        assignEncadrantProToOffre(offre, request, entreprise, authenticatedUser);
    }

    private void assignEncadrantProToOffre(
            OffreStage offre,
            CreateOffreStageRequest request,
            Entreprise entreprise,
            Utilisateur authenticatedUser
    ) {
        Long encId = request.getEncadrantProId();
        if (encId == null) {
            if (authenticatedUser.getRole() == Role.RESPONSABLE_ENTREPRISE) {
                throw new IllegalArgumentException("L'encadrant professionnel est obligatoire.");
            }
            offre.setEncadrantPro(null);
            return;
        }

        EncadrantProfessionnel enc = encadrantProfessionnelRepository.findById(encId)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant professionnel introuvable."));

        Long entId = entreprise != null ? entreprise.getId() : null;
        Long encEntId = enc.getEntreprise() != null ? enc.getEntreprise().getId() : null;
        if (entId == null || encEntId == null || !entId.equals(encEntId)) {
            throw new IllegalArgumentException("L'encadrant professionnel selectionne n'appartient pas a cette entreprise.");
        }

        offre.setEncadrantPro(enc);
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
        return authenticatedUser.getRole() == Role.RESPONSABLE_STAGE
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
        // Affectation active = stage lie existant ET non annule (exclut un stage cancelle).
        Stage activeStage = (linkedStage != null && linkedStage.getStatut() != StatutStage.ANNULE)
                ? linkedStage
                : null;
        boolean affectationActive = activeStage != null;
        LocalDate dateFinStage = activeStage != null ? calculateStageEndDate(activeStage) : null;
        boolean stageTermine = activeStage != null && isStageTermine(activeStage);
        String statutSujetName = activeStage != null && activeStage.getStatutSujet() != null
                ? activeStage.getStatutSujet().name()
                : null;
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
                .encadrantProId(offre.getEncadrantPro() != null ? offre.getEncadrantPro().getId() : null)
                .encadrantProNomComplet(buildFullName(offre.getEncadrantPro()))
                .stageCree(stageCree)
                .dateFinStage(dateFinStage)
                .stageTermine(stageTermine)
                .stageDeclenche(stageDeclenche)
                .trelloEnabled(linkedStage != null && linkedStage.getStatut() == StatutStage.EN_COURS)
                .affectable(isOfferAssignable(offre, hasCreatedStage(offre)))
                .affectationActive(affectationActive)
                .statutSujet(statutSujetName)
                .annulationAffectationAutorisee(isAnnulationAffectationAutorisee(offre, activeStage))
                .build();
    }

    /**
     * Annulation par le responsable entreprise : autorisee tant que le sujet n'est pas valide
     * par l'encadrant academique et que la date de debut du stage est strictement future.
     */
    private boolean isAnnulationAffectationAutorisee(OffreStage offre, Stage activeStage) {
        if (offre == null || offre.getStagiaireAffecte() == null) {
            return false;
        }
        if (activeStage == null) {
            return true;
        }
        if (isStageTermine(activeStage)) {
            return false;
        }
        if (activeStage.getStatutSujet() == StatutValidation.VALIDEE) {
            return false;
        }
        LocalDate dateDebut = activeStage.getDateDebut() != null
                ? activeStage.getDateDebut()
                : offre.getDateDebutPrevue();
        if (dateDebut == null) {
            StatutStage statut = activeStage.getStatut();
            return statut == StatutStage.PAS_COMMENCE || statut == StatutStage.A_VENIR;
        }
        return dateDebut.isAfter(LocalDate.now());
    }

    private String buildAnnulationAffectationRefusalMessage(Stage stage) {
        if (stage.getStatutSujet() == StatutValidation.VALIDEE) {
            return "Impossible d\u2019annuler l\u2019affectation : le sujet de stage a d\u00e9j\u00e0 \u00e9t\u00e9 valid\u00e9 par l\u2019encadrant acad\u00e9mique.";
        }
        if (isStageTermine(stage)) {
            return MSG_OFFRE_LOCKED_STAGE_TERMINE;
        }
        return "Impossible d\u2019annuler l\u2019affectation : la date de d\u00e9but du stage est atteinte ou le stage est d\u00e9j\u00e0 en cours.";
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
            throw new IllegalArgumentException("Tous les champs obligatoires doivent étre renseignés.");
        }
        return normalized;
    }

    private boolean isOfferApprovalAuthorizedRole(Role role) {
        return role == Role.RESPONSABLE_STAGE
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

    private static final String MSG_OFFRE_LOCKED_STAGE_TERMINE = "Modification impossible : stage terminé";

    /**
     * Verrouille toute modification d'offre lorsque le stage associe est termine
     * (date du jour >= date de fin du stage), pour tous les roles.
     */
    private void ensureOfferNotLockedByTerminatedStage(OffreStage offre) {
        Stage linkedStage = findLatestStageForOffer(offre.getId());
        if (linkedStage == null || linkedStage.getStatut() == StatutStage.ANNULE) {
            return;
        }

        if (isStageTermine(linkedStage)) {
            throw new IllegalStateException(MSG_OFFRE_LOCKED_STAGE_TERMINE);
        }
    }

    private boolean isStageTermine(Stage stage) {
        if (stage == null || stage.getStatut() == StatutStage.ANNULE) {
            return false;
        }
        LocalDate dateFin = calculateStageEndDate(stage);
        if (dateFin == null) {
            return false;
        }
        return !LocalDate.now().isBefore(dateFin);
    }

    private void ensureOfferCanBeUpdated(OffreStage offre, Utilisateur authenticatedUser) {
        // Note : on n'invoque plus ensureOffreNotAssigned ici. Un etudiant peut etre affecte
        // sans que le sujet ait ete valide par l'encadrant academique — dans ce cas le
        // Responsable Entreprise garde la libre modification (regle metier explicite).
        // Le blocage post-validation est gere en amont dans updateOffre().

        if (authenticatedUser == null || authenticatedUser.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            return;
        }

        if (offre.getStatut() == StatutOffre.FERMEE) {
            throw new IllegalStateException("Cette offre n'est pas modifiable selon son statut actuel.");
        }

        if (offre.getStatut() == StatutOffre.TERMINEE || offre.getStatut() == StatutOffre.ARCHIVEE) {
            throw new IllegalStateException("Cette offre est terminée et ne peut plus être modifiée.");
        }

        if (offre.getStatut() == null) {
            throw new IllegalStateException("Cette offre n'est pas modifiable selon son statut actuel.");
        }
    }

    /**
     * Vérifie que la date du jour se situe dans l'une des deux périodes autorisées
     * pour la création d'offres de stage :
     *   - Période 1 : du 1er février au 1er juin (inclus)
     *   - Période 2 : du 1er juin au 30 août (inclus)
     * Ces deux périodes forment une fenêtre continue du 1er février au 30 août.
     *
     * Levée uniquement pour le rôle RESPONSABLE_ENTREPRISE ; les administrateurs
     * et responsables des stages ne sont pas soumis à cette restriction.
     */
    private void validateCreationPeriod() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue(); // 1–12
        int day   = today.getDayOfMonth();

        // Fenêtre ouverte : 1er février → 30 août inclus
        boolean inPeriod = month >= 2 && (month < 8 || (month == 8 && day <= 30));

        if (!inPeriod) {
            // Calcul de la prochaine ouverture (1er février)
            int nextYear = (month >= 9) ? today.getYear() + 1 : today.getYear();
            String nextOpening = "1er fevrier " + nextYear;
            throw new IllegalStateException(
                "La creation d'offres de stage est actuellement fermee. " +
                "Periodes autorisees : du 1er fevrier au 1er juin, et du 1er juin au 30 aout. " +
                "Prochaine ouverture : " + nextOpening + "."
            );
        }
    }

    private void validateOfferRequest(CreateOffreStageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Tous les champs obligatoires doivent étre renseignés.");
        }

        if (normalizeText(request.getTitre()) == null
                || normalizeText(request.getDescriptionMissions()) == null
                || request.getDateDebutPrevue() == null) {
            throw new IllegalArgumentException("Tous les champs obligatoires doivent étre renseignés.");
        }

        if (normalizeText(request.getTitre()).length() < 3
                || normalizeText(request.getDescriptionMissions()).length() < 10) {
            throw new IllegalArgumentException("Les données saisies sont invalides.");
        }

        Integer duree = request.getDuree();
        if (duree == null) {
            throw new IllegalArgumentException(
                    "La durée du stage est obligatoire (1 à 3 mois en période 1, 1 à 2 mois en période 2).");
        }

        StagePeriodValidation.validatePeriod(request.getDateDebutPrevue(), duree);

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

    /**
     * Vérifie qu'aucun stage existant n'est ACTIF aujourd'hui pour ce stagiaire.
     *
     * Règle métier : l'affectation est refusée si la date du jour est comprise entre
     * dateDebut et dateFin (bornes incluses) d'un stage existant non annulé/non refusé.
     */
    private void existsStageActifByStagiaire(Stagiaire stagiaire, OffreStage offre) {
        LocalDate today = LocalDate.now();
        List<Stage> stagesNonRefusesNonAnnules = stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(stage -> stage.getStatut() != null)
                .filter(stage -> stage.getStatut() != StatutStage.REFUSE)
                .filter(stage -> stage.getStatut() != StatutStage.ANNULE)
                .toList();

        stagesNonRefusesNonAnnules.stream()
                .filter(stage -> isDateWithinStagePeriod(today, stage))
                .findFirst()
                .ifPresent(stageActif -> {
                    log.warn(
                            "Affectation refusee - le stagiaire {} est deja en periode active " +
                                    "(stageId={}, dateDebut={}, dateFin={})",
                            stagiaire.getId(), stageActif.getId(), stageActif.getDateDebut(), stageActif.getDateFin()
                    );
                    throw new IllegalStateException(
                            "Ce stagiaire ne peut pas etre affecte a une nouvelle offre car il est deja "
                                    + "dans une periode de stage active."
                    );
                });

    }

    private void ensureNoExistingOfferAssignment(Stagiaire stagiaire, OffreStage offreCourante) {
        if (stagiaire == null || stagiaire.getId() == null || offreCourante == null || offreCourante.getId() == null) {
            return;
        }

        boolean alreadyAssignedElsewhere = offreStageRepository.findAll().stream()
                .filter(offre -> offre.getId() != null && !offre.getId().equals(offreCourante.getId()))
                .filter(offre -> offre.getStatut() == StatutOffre.AFFECTEE)
                .anyMatch(offre -> offre.getStagiaireAffecte() != null
                        && stagiaire.getId().equals(offre.getStagiaireAffecte().getId()));

        if (alreadyAssignedElsewhere) {
            throw new IllegalStateException("Cet etudiant est deja affecte a une autre offre. Annulez l'affectation existante avant de continuer.");
        }
    }

    private LocalDate calculateStageEndDate(Stage stage) {
        if (stage.getDateFin() != null) {
            return stage.getDateFin();
        }

        if (stage.getDateDebut() == null) {
            return null;
        }

        if (stage.getDuree() != null && stage.getDuree() > 0) {
            return StagePeriodValidation.calculateEndDate(stage.getDateDebut(), stage.getDuree());
        }

        return stage.getDateDebut();
    }

    private boolean isDateWithinStagePeriod(LocalDate date, Stage stage) {
        if (date == null || stage == null || stage.getDateDebut() == null) {
            return false;
        }
        LocalDate stageEnd = calculateStageEndDate(stage);
        if (stageEnd == null) {
            return false;
        }
        return !date.isBefore(stage.getDateDebut()) && !date.isAfter(stageEnd);
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

    private void logOfferControlledUpdateHistory(OffreStage offre, String oldDescription, LocalDate oldDateDebut) {
        if (offre == null) {
            return;
        }
        log.info(
                "Historique modification offre {} : description [{}] -> [{}], dateDebut [{}] -> [{}]",
                offre.getId(),
                oldDescription,
                offre.getDescriptionMissions(),
                oldDateDebut,
                offre.getDateDebutPrevue()
        );
    }

    private void notifyActorsAfterOfferUpdate(OffreStage offre, Stage linkedStage, Utilisateur auteur) {
        if (offre == null || linkedStage == null || linkedStage.getId() == null) {
            return;
        }
        String auteurNom = auteur == null ? "Representant entreprise" : buildFullName(auteur);
        String titreNormalise = normalizeText(offre.getTitre());
        String titreOffre = titreNormalise != null ? titreNormalise : ("offre #" + offre.getId());
        String message = "L'offre \"" + titreOffre + "\" a ete mise a jour par " + auteurNom
                + " (description et/ou date de debut) avant validation academique du sujet.";

        Set<Long> destinataires = new LinkedHashSet<>();
        if (linkedStage.getStagiaire() != null && linkedStage.getStagiaire().getId() != null) {
            destinataires.add(linkedStage.getStagiaire().getId());
        }
        if (linkedStage.getEncadrantAcademique() != null && linkedStage.getEncadrantAcademique().getId() != null) {
            destinataires.add(linkedStage.getEncadrantAcademique().getId());
        }
        utilisateurRepository.findByRole(Role.RESPONSABLE_STAGE).stream()
                .map(Utilisateur::getId)
                .filter(java.util.Objects::nonNull)
                .forEach(destinataires::add);

        destinataires.forEach(destinataireId ->
                notificationService.creerNotification(
                        destinataireId,
                        "Mise a jour de l'offre de stage",
                        message,
                        "MISE_A_JOUR_OFFRE_AVANT_VALIDATION_SUJET",
                        linkedStage.getId(),
                        "STAGE"
                )
        );
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
                .map(convention -> !convention.getSignatures().isEmpty())
                .orElse(false);

        boolean cahierSigne = cahierStageRepository.findByStageId(stageId)
                .map(cahier -> !cahier.getSignatures().isEmpty())
                .orElse(false);

        boolean ficheSignee = ficheEvaluationRepository.findFirstByStageId(stageId)
                .map(fiche -> !fiche.getSignatures().isEmpty())
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
