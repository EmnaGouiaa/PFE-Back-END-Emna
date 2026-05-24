package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AffectationEncadrantAcademiqueResponse;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.StagiaireResponseDTO;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.StagiaireMapper;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.FiliereRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StagiaireServiceImpl implements StagiaireService {
    private final StagiaireRepository stagiaireRepository;
    private final StagiaireMapper stagiaireMapper;
    private final FiliereRepository filiereRepository;
    private final EncadrantAcademiqueRepository encadrantAcademiqueRepository;
    private final StageRepository stageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;
    private final EntityManager entityManager;
    private final StagiaireResolutionService stagiaireResolutionService;
    private final NotificationService notificationService;

    @Override
    public StagiaireResponseDTO create(StagiaireRequestDTO dto) {
        if (stagiaireRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        if (dto.getMatricule() == null || dto.getMatricule().isBlank()) {
            throw new IllegalArgumentException("Le matricule est obligatoire.");
        }
        if (dto.getFiliereId() == null) {
            throw new IllegalArgumentException("La filière est obligatoire.");
        }

        if (stagiaireRepository.existsByMatricule(dto.getMatricule())) {
            throw new RuntimeException("Matricule déjà utilisé");
        }

        Stagiaire stagiaire = stagiaireMapper.toEntity(dto);

        if (dto.getFiliereId() != null) {
            stagiaire.setFiliere(
                    filiereRepository.findById(dto.getFiliereId())
                            .orElseThrow(() -> new RuntimeException("Filiere introuvable"))
            );
        }

        Stagiaire saved = stagiaireRepository.save(stagiaire);

        return toResponseDTO(saved);
    }

    @Override
    public StagiaireResponseDTO update(Long id, StagiaireRequestDTO dto) {
        Stagiaire existing = stagiaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stagiaire introuvable avec id : " + id));

        if (dto.getEmail() != null &&
                !dto.getEmail().equals(existing.getEmail()) &&
                stagiaireRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email déjé utilisé");
        }

        if (dto.getMatricule() != null &&
                !dto.getMatricule().equals(existing.getMatricule()) &&
                stagiaireRepository.existsByMatricule(dto.getMatricule())) {
            throw new RuntimeException("Matricule déjé utilisé");
        }

        stagiaireMapper.updateEntityFromDto(dto, existing);

        if (dto.getFiliereId() != null) {
            existing.setFiliere(
                    filiereRepository.findById(dto.getFiliereId())
                            .orElseThrow(() -> new RuntimeException("Filiere introuvable"))
            );
        }

        Stagiaire updated = stagiaireRepository.save(existing);

        return toResponseDTO(updated);
    }

    @Override
    public StagiaireResponseDTO getById(Long id) {
        synchroniserStagiairesLegacy();

        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stagiaire introuvable avec id : " + id));

        return toResponseDTO(stagiaire);
    }

    @Override
    public List<StagiaireResponseDTO> getAll() {
        synchroniserStagiairesLegacy();

        return stagiaireRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<StagiaireResponseDTO> getSansStage() {
        synchroniserStagiairesLegacy();

        return stagiaireRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .filter(stagiaire -> !Boolean.TRUE.equals(stagiaire.getHasStageActif()))
                .toList();
    }

    @Override
    public StagiaireResponseDTO searchByEmail(String email) {
        return toResponseDTO(stagiaireResolutionService.findByEmail(email));
    }

    @Override
    public AffectationEncadrantAcademiqueResponse affecterEncadrantAcademique(Long stagiaireId, Long encadrantId) {
        ensureAuthenticatedFacultyManager();
        synchroniserStagiairesLegacy();

        Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant academique introuvable"));

        boolean alreadyAssigned = stagiaire.getEncadrantAcademique() != null
                && encadrantId.equals(stagiaire.getEncadrantAcademique().getId());
        if (alreadyAssigned) {
            return AffectationEncadrantAcademiqueResponse.builder()
                    .message("Cet encadrant academique est deja affecte a cet etudiant.")
                    .stagiaire(toResponseDTO(stagiaire))
                    .build();
        }

        boolean isModification = stagiaire.getEncadrantAcademique() != null;
        stagiaire.setEncadrantAcademique(encadrant);
        Stagiaire saved = stagiaireRepository.save(stagiaire);

        synchroniserStagesActifs(saved, encadrant);
        notifierEncadrantAffecteAuxStagesActifs(saved, encadrant, isModification);
        notifierStagiaire(saved, encadrant, isModification);

        return AffectationEncadrantAcademiqueResponse.builder()
                .message("Encadrant academique affecte avec succes")
                .stagiaire(toResponseDTO(saved))
                .build();
    }

    @Override
    public void delete(Long id) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stagiaire introuvable avec id : " + id));

        // Supprimer tous les stages liés avant de supprimer le stagiaire.
        // La cascade JPA sur Stage (conventions, notifications, absences, rapports, etc.)
        // est déclenchée automatiquement par stageRepository.delete().
        List<Stage> stages = stageRepository.findByStagiaireId(id);
        for (Stage stage : stages) {
            supprimerStageComplet(stage);
        }

        stagiaireRepository.delete(stagiaire);
    }

    /**
     * Au démarrage, nettoie les stages orphelins dont le stagiaire a déjà été supprimé
     * (stagiaire_id IS NULL — résidu de l'ancienne implémentation de delete).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void nettoyerStagesOrphelins() {
        List<Stage> orphelins = stageRepository.findByStagiaireIsNull();
        if (orphelins.isEmpty()) {
            return;
        }
        log.info("Nettoyage de {} stage(s) orphelin(s) sans stagiaire...", orphelins.size());
        for (Stage stage : orphelins) {
            supprimerStageComplet(stage);
        }
        log.info("Nettoyage des stages orphelins termine.");
    }

    /**
     * Supprime un stage en gérant correctement :
     * - la table de jointure reunion_participants (@ManyToMany)
     * - toutes les entités liées via cascade JPA (convention, notifications, absences, rapports, etc.)
     */
    private void supprimerStageComplet(Stage stage) {
        // Vider la table de jointure reunion_participants avant la suppression des réunions
        if (stage.getReunions() != null) {
            stage.getReunions().forEach(reunion -> {
                if (reunion.getParticipants() != null) {
                    reunion.getParticipants().clear();
                }
            });
        }
        stageRepository.delete(stage);
    }

    private StagiaireResponseDTO toResponseDTO(Stagiaire stagiaire) {
        StagiaireResponseDTO base = stagiaireMapper.toResponseDTO(stagiaire);
        Stage stageActif = findStageActif(stagiaire);

        return base.toBuilder()
                .stageActifId(stageActif != null ? stageActif.getId() : null)
                .stageActifTitre(stageActif != null ? stageActif.getTitre() : null)
                .stageActifStatut(stageActif != null && stageActif.getStatut() != null ? stageActif.getStatut().name() : null)
                .hasStageActif(stageActif != null)
                .build();
    }

    private Stage findStageActif(Stagiaire stagiaire) {
        if (stagiaire == null || stagiaire.getId() == null) {
            return null;
        }

        return stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(this::isStageActif)
                .sorted(Comparator
                        .comparingInt(this::getStagePriority)
                        .thenComparing(Stage::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private void synchroniserStagesActifs(Stagiaire stagiaire, EncadrantAcademique encadrant) {
        if (stagiaire == null || stagiaire.getId() == null) {
            return;
        }

        List<Stage> stagesActifs = stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(this::isStageActif)
                .toList();

        boolean updated = false;
        for (Stage stage : stagesActifs) {
            Long currentId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
            if (encadrant != null && !encadrant.getId().equals(currentId)) {
                stage.setEncadrantAcademique(encadrant);
                updated = true;
            }
        }

        if (updated) {
            stageRepository.saveAll(stagesActifs);
        }
    }

    private boolean isStageActif(Stage stage) {
        return stage != null
                && stage.getStatut() != null
                && stage.getStatut() != StatutStage.REFUSE
                && stage.getStatut() != StatutStage.ANNULE
                && stage.getStatut() != StatutStage.TERMINE;
    }

    private int getStagePriority(Stage stage) {
        if (stage == null || stage.getStatut() == null) {
            return 99;
        }

        return switch (stage.getStatut()) {
            case EN_COURS -> 0;
            case A_VENIR, PAS_COMMENCE -> 1;
            case TERMINE -> 4;
            case ANNULE -> 5;
            case REFUSE -> 5;
        };
    }

    private void notifierStagiaire(Stagiaire stagiaire, EncadrantAcademique encadrant, boolean isModification) {
        if (stagiaire == null || stagiaire.getId() == null) {
            return;
        }
        String titre = isModification ? "Encadrant academique modifie" : "Encadrant academique affecte";
        String message = isModification
                ? "Votre encadrant academique a ete mis a jour."
                : "Un encadrant academique a ete affecte a votre dossier.";
        String type = isModification ? "MODIFICATION_ENCADRANT_ACADEMIQUE" : "AFFECTATION_ENCADRANT_ACADEMIQUE";
        notificationService.creerNotification(stagiaire.getId(), titre, message, type, null, null);
    }

    private void notifierEncadrantAffecteAuxStagesActifs(Stagiaire stagiaire, EncadrantAcademique encadrant, boolean isModification) {
        if (stagiaire == null || stagiaire.getId() == null || encadrant == null || encadrant.getId() == null) {
            return;
        }

        stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(this::isStageActif)
                .filter(stage -> stage.getEncadrantAcademique() != null)
                .filter(stage -> encadrant.getId().equals(stage.getEncadrantAcademique().getId()))
                .forEach(stage -> notificationService.creerNotification(
                        encadrant.getId(),
                        isModification ? "Changement d'affectation encadrant" : "Nouvelle affectation encadrant",
                        isModification
                                ? "Votre affectation comme encadrant academique a ete mise a jour pour un stage."
                                : "Vous avez ete affecte comme encadrant academique pour un stage.",
                        isModification ? "MODIFICATION_ENCADRANT_ACADEMIQUE" : "AFFECTATION_ENCADRANT_ACADEMIQUE",
                        stage.getId(),
                        "STAGE"
                ));
    }

    private void ensureAuthenticatedFacultyManager() {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Action non autorisee"));

        Set<Role> allowedRoles = Set.of(
                Role.RESPONSABLE_STAGE,
                Role.ADMINISTRATEUR
        );

        if (!allowedRoles.contains(utilisateur.getRole())) {
            throw new AccessDeniedException("Action non autorisee");
        }
    }

    @Transactional
    protected void synchroniserStagiairesLegacy() {
        utilisateurRepository.findByRole(Role.STAGIAIRE)
                .stream()
                .filter(utilisateur -> !stagiaireRepository.existsById(utilisateur.getId()))
                .forEach(utilisateur -> {
                    entityManager.createNativeQuery("""
                            INSERT INTO stagiaire (id)
                            SELECT :id
                            WHERE NOT EXISTS (SELECT 1 FROM stagiaire WHERE id = :id)
                            """)
                            .setParameter("id", utilisateur.getId())
                            .executeUpdate();

                    entityManager.createNativeQuery("""
                            UPDATE utilisateur
                            SET dtype = 'Stagiaire'
                            WHERE id = :id AND (dtype IS NULL OR dtype <> 'Stagiaire')
                            """)
                            .setParameter("id", utilisateur.getId())
                            .executeUpdate();
                });

        entityManager.flush();
        entityManager.clear();
    }
}
