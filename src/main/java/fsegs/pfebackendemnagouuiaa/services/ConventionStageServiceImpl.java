package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ConventionStageMapper;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ConventionStageServiceImpl implements ConventionStageService {

    private final ConventionStageRepository conventionStageRepository;
    private final StageRepository stageRepository;
    private final ConventionStageMapper conventionStageMapper;
    private final JwtService jwtService;

    @Override
    public ConventionStageDto create(ConventionStageDto dto) {
        ConventionStage entity = conventionStageMapper.toEntity(dto);

        if (dto.getStageId() != null) {
            Stage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));
            entity.setStage(stage);
        }

        if (entity.getNumConv() == null) {
            entity.setNumConv(generateNumConv());
        }

        return conventionStageMapper.toDto(conventionStageRepository.save(entity));
    }

    @Override
    public ConventionStageDto createByStage(Long stageId, ConventionStageDto dto) {
        if (conventionStageRepository.existsByStageId(stageId)) {
            throw new RuntimeException("Une convention existe déjà pour ce stage");
        }

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + stageId));

        ConventionStage entity = conventionStageMapper.toEntity(dto);
        entity.setStage(stage);

        if (entity.getNumConv() == null) entity.setNumConv(generateNumConv());
        if (entity.getDateDebut() == null) entity.setDateDebut(stage.getDateDebut());
        if (entity.getDateFin() == null) entity.setDateFin(stage.getDateFin());

        return conventionStageMapper.toDto(conventionStageRepository.save(entity));
    }

    @Override
    public ConventionStageDto getById(Long id) {
        return conventionStageMapper.toDto(conventionStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convention introuvable avec l'id : " + id)));
    }

    @Override
    public ConventionStageDto getByStageId(Long stageId) {
        return conventionStageMapper.toDto(conventionStageRepository.findByStageId(stageId)
                .orElseThrow(() -> new RuntimeException("Convention introuvable pour le stage id : " + stageId)));
    }

    @Override
    public List<ConventionStageDto> getAll() {
        return conventionStageRepository.findAll().stream().map(conventionStageMapper::toDto).toList();
    }

    @Override
    public ConventionStageDto update(Long id, ConventionStageDto dto) {
        ConventionStage entity = conventionStageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document introuvable."));
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
        return conventionStageMapper.toDto(conventionStageRepository.save(entity));
    }

    @Override
    public ConventionStageDto signerParStagiaire(Long id) {
        return signer(id, TypeSignature.STAGIAIRE);
    }

    @Override
    public ConventionStageDto signerParEncadrantAcademique(Long id) {
        return signer(id, TypeSignature.ENCADRANT_ACADEMIQUE);
    }

    @Override
    public ConventionStageDto signerParEncadrantProfessionnel(Long id) {
        return signer(id, TypeSignature.ENCADRANT_PROFESSIONNEL);
    }

    @Override
    public ConventionStageDto signerParEntreprise(Long id) {
        return signer(id, TypeSignature.ENTREPRISE);
    }

    @Override
    public ConventionStageDto signerParResponsable(Long id) {
        return signer(id, TypeSignature.RESPONSABLE);
    }

    @Override
    public void delete(Long id) {
        ConventionStage convention = conventionStageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document introuvable."));
        conventionStageRepository.delete(convention);
    }

    // ── Logique de signature ───────────────────────────────────────────────────

    private ConventionStageDto signer(Long id, TypeSignature typeSignature) {
        // E2 — Document introuvable (404 via GlobalExceptionHandler)
        ConventionStage convention = conventionStageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document introuvable."));

        Utilisateur utilisateur = getAuthenticatedSigner(typeSignature);
        ensureSignerBelongsToStage(convention.getStage(), utilisateur, typeSignature);

        RoleSignature role = roleSignature(typeSignature);

        // E5 — Document déjà signé par cet utilisateur (400)
        if (convention.estSignePar(role)) {
            throw new IllegalArgumentException("Vous avez déjà signé ce document.");
        }

        // E4 — Pas de signature dans le profil (400)
        String urlSignature = utilisateur.getUrlSignature();
        if (urlSignature == null || urlSignature.isBlank()) {
            throw new IllegalArgumentException("Veuillez enregistrer votre signature dans votre profil avant de continuer.");
        }

        Signature sig = new Signature();
        sig.setRoleSignature(role);
        sig.setSignataireId(utilisateur.getId());
        sig.setDateSignature(LocalDateTime.now());
        sig.setUrlSignature(urlSignature);                  // ← persistance de la signature
        convention.getSignatures().add(sig);

        return conventionStageMapper.toDto(conventionStageRepository.save(convention));
    }

    private Utilisateur getAuthenticatedSigner(TypeSignature typeSignature) {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new RuntimeException("Utilisateur authentifie introuvable."));

        if (utilisateur.getRole() != expectedRole(typeSignature)) {
            throw new RuntimeException("Action non autorisee : votre role ne correspond pas a cette signature.");
        }

        return utilisateur;
    }

    private void ensureSignerBelongsToStage(Stage stage, Utilisateur utilisateur, TypeSignature typeSignature) {
        if (stage == null) throw new RuntimeException("Aucun stage n'est associe a cette convention.");

        boolean authorized = switch (typeSignature) {
            case STAGIAIRE -> stage.getStagiaire() != null && stage.getStagiaire().getId().equals(utilisateur.getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId().equals(utilisateur.getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
            case ENTREPRISE -> {
                if (!(utilisateur instanceof ResponsableEntreprise re)) yield false;
                yield re.getEntreprise() != null
                        && stage.getEntreprise() != null
                        && re.getEntreprise().getId().equals(stage.getEntreprise().getId());
            }
            case RESPONSABLE -> true;
        };

        if (!authorized) throw new RuntimeException("Action non autorisee : vous n'etes pas associe a ce document.");
    }

    private Role expectedRole(TypeSignature t) {
        return switch (t) {
            case STAGIAIRE -> Role.STAGIAIRE;
            case ENCADRANT_ACADEMIQUE -> Role.ENCADRANT_ACADEMIQUE;
            case ENCADRANT_PROFESSIONNEL -> Role.ENCADRANT_PROFESSIONNEL;
            case ENTREPRISE -> Role.RESPONSABLE_ENTREPRISE;
            case RESPONSABLE -> Role.RESPONSABLE_STAGE;
        };
    }

    private RoleSignature roleSignature(TypeSignature t) {
        return switch (t) {
            case STAGIAIRE -> RoleSignature.STAGIAIRE;
            case ENCADRANT_ACADEMIQUE -> RoleSignature.ENCADRANT_ACADEMIQUE;
            case ENCADRANT_PROFESSIONNEL -> RoleSignature.ENCADRANT_PROFESSIONNEL;
            case ENTREPRISE -> RoleSignature.RESPONSABLE_ENTREPRISE;
            case RESPONSABLE -> RoleSignature.RESPONSABLE_UNIVERSITAIRE;
        };
    }

    private void requireSavedSignature(Utilisateur utilisateur) {
        if (utilisateur.getUrlSignature() == null || utilisateur.getUrlSignature().isBlank()) {
            throw new RuntimeException("Veuillez ajouter votre signature à votre profil avant de signer ce document.");
        }
    }

    private Integer generateNumConv() {
        int num;
        Random random = new Random();
        do {
            num = 100000 + random.nextInt(900000);
        } while (conventionStageRepository.findByNumConv(num).isPresent());
        return num;
    }

    private enum TypeSignature {
        STAGIAIRE, ENCADRANT_ACADEMIQUE, ENCADRANT_PROFESSIONNEL, ENTREPRISE, RESPONSABLE
    }
}
