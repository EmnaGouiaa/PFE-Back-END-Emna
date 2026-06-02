package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.dto.SignerCahierRequest;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.CahierStageMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CahierStageServiceImpl implements CahierStageService {

    private final CahierStageRepository cahierStageRepository;
    private final StageRepository stageRepository;
    private final CahierStageMapper cahierStageMapper;
    private final JwtService jwtService;

    @Override
    public CahierStageDto create(CahierStageDto dto) {
        CahierStage entity = cahierStageMapper.toEntity(dto);

        if (dto.getStageId() != null) {
            Stage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));
            entity.setStage(stage);
        }

        if (entity.getDateGeneration() == null) {
            entity.setDateGeneration(LocalDate.now());
        }

        return cahierStageMapper.toDto(cahierStageRepository.save(entity));
    }

    @Override
    public CahierStageDto createByStage(Long stageId, CahierStageDto dto) {
        if (cahierStageRepository.existsByStageId(stageId)) {
            throw new RuntimeException("Un cahier existe déjà pour ce stage");
        }

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + stageId));

        CahierStage entity = cahierStageMapper.toEntity(dto);
        entity.setStage(stage);

        if (entity.getDateGeneration() == null) {
            entity.setDateGeneration(LocalDate.now());
        }

        return cahierStageMapper.toDto(cahierStageRepository.save(entity));
    }

    @Override
    public CahierStageDto getById(Long id) {
        CahierStage entity = cahierStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable avec l'id : " + id));
        return cahierStageMapper.toDto(entity);
    }

    @Override
    public Optional<CahierStageDto> findByStageIdIfPresent(Long stageId) {
        return cahierStageRepository.findByStageId(stageId).map(cahierStageMapper::toDto);
    }

    @Override
    public CahierStageDto getByStageId(Long stageId) {
        return findByStageIdIfPresent(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Cahier introuvable pour le stage id : " + stageId));
    }

    @Override
    public List<CahierStageDto> getAll() {
        return cahierStageRepository.findAll()
                .stream()
                .map(cahierStageMapper::toDto)
                .toList();
    }

    @Override
    public CahierStageDto update(Long id, CahierStageDto dto) {
        CahierStage entity = cahierStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable avec l'id : " + id));

        entity.setDateGeneration(dto.getDateGeneration());
        entity.setDateSignature(dto.getDateSignature());

        return cahierStageMapper.toDto(cahierStageRepository.save(entity));
    }

    @Override
    public CahierStageDto signerParStagiaire(Long id, SignerCahierRequest request) {
        return signer(id, TypeSignature.STAGIAIRE, request);
    }

    @Override
    public CahierStageDto signerParEncadrantAcademique(Long id, SignerCahierRequest request) {
        return signer(id, TypeSignature.ENCADRANT_ACADEMIQUE, request);
    }

    @Override
    public CahierStageDto signerParEncadrantProfessionnel(Long id, SignerCahierRequest request) {
        return signer(id, TypeSignature.ENCADRANT_PROFESSIONNEL, request);
    }

    @Override
    public CahierStageDto signerParResponsableEntreprise(Long id, SignerCahierRequest request) {
        return signer(id, TypeSignature.RESPONSABLE_ENTREPRISE, request);
    }

    @Override
    public void delete(Long id) {
        CahierStage entity = cahierStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable avec l'id : " + id));
        cahierStageRepository.delete(entity);
    }

    // ── Logique de signature ───────────────────────────────────────────────────

    private CahierStageDto signer(Long id, TypeSignature typeSignature, SignerCahierRequest request) {
        // ── 1. Cahier de stage charge (404 si absent via global exception handler) ───────
        CahierStage cahier = cahierStageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cahier introuvable avec l'id : " + id));

        Utilisateur utilisateur = getAuthenticatedSigner(typeSignature);
        ensureSignerBelongsToStage(cahier.getStage(), utilisateur, typeSignature);

        StageDocumentSignatureRules.ensureLogbookSigningAllowed(cahier.getStage());

        RoleSignature role = roleSignature(typeSignature);

        // Idempotence : déjà signé → retour sans erreur
        if (cahier.estSignePar(role)) {
            log.info("AUDIT signature - requete idempotente (deja signe). cahierId={}, role={}", cahier.getId(), role);
            return cahierStageMapper.toDto(cahier);
        }

        // ── 3. Resolution de l'image de signature ─────────────────────────────────────────
        // Priorite : (a) image fournie dans la requete si non vide, (b) sinon image stockee
        // sur le profil de l'utilisateur authentifie. Si aucune source n'est disponible :
        // erreur explicite invitant a renseigner la signature de profil.
        String signatureImage = null;
        if (request != null && request.getSignatureImage() != null && !request.getSignatureImage().isBlank()) {
            signatureImage = request.getSignatureImage().trim();
        } else if (utilisateur.getUrlSignature() != null && !utilisateur.getUrlSignature().isBlank()) {
            signatureImage = utilisateur.getUrlSignature().trim();
        }

        if (signatureImage == null) {
            // E4 — aucune signature disponible (ni body, ni profil)
            log.warn("AUDIT signature - aucune image disponible. cahierId={}, utilisateurId={}",
                    cahier.getId(), utilisateur.getId());
            throw new IllegalArgumentException("Veuillez enregistrer votre signature dans votre profil avant de continuer.");
        }

        // Validation legere du format si une image est fournie (pas une URL relative bidon).
        if (!signatureImage.startsWith("data:image/")
                && !signatureImage.startsWith("http://")
                && !signatureImage.startsWith("https://")
                && !signatureImage.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Format d'image de signature invalide : data URL (data:image/...) ou URL attendue.");
        }

        // ── 4. Creation de la signature avec image + horodatage + identite ────────────────
        Signature sig = new Signature();
        sig.setRoleSignature(role);
        sig.setSignataireId(utilisateur.getId());
        sig.setDateSignature(LocalDateTime.now());
        sig.setUrlSignature(signatureImage);
        cahier.getSignatures().add(sig);

        cahier.setDateSignature(LocalDate.now());

        CahierStageDto result = cahierStageMapper.toDto(cahierStageRepository.save(cahier));

        // ── 5. Audit log (minimal via slf4j ; remplacable par une table dediee) ───────────
        log.info("AUDIT signature - cahier signe. cahierId={}, role={}, utilisateurId={}, email={}, dateSignature={}",
                cahier.getId(), role, utilisateur.getId(),
                utilisateur.getEmail(),
                sig.getDateSignature());

        return result;
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
        if (stage == null) throw new RuntimeException("Aucun stage n'est associe a ce cahier.");

        boolean authorized = switch (typeSignature) {
            case STAGIAIRE -> stage.getStagiaire() != null && stage.getStagiaire().getId().equals(utilisateur.getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId().equals(utilisateur.getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
            case RESPONSABLE_ENTREPRISE -> {
                // Check that the logged-in user belongs to the same company as the stage,
                // not that they are the specific tuteurEntreprise person (who may be null).
                if (!(utilisateur instanceof ResponsableEntreprise re)) yield false;
                yield stage.getEntreprise() != null
                        && re.getEntreprise() != null
                        && stage.getEntreprise().getId().equals(re.getEntreprise().getId());
            }
        };

        if (!authorized) throw new RuntimeException("Action non autorisee : vous n'etes pas associe a ce document.");
    }

    private Role expectedRole(TypeSignature typeSignature) {
        return switch (typeSignature) {
            case STAGIAIRE -> Role.STAGIAIRE;
            case ENCADRANT_ACADEMIQUE -> Role.ENCADRANT_ACADEMIQUE;
            case ENCADRANT_PROFESSIONNEL -> Role.ENCADRANT_PROFESSIONNEL;
            case RESPONSABLE_ENTREPRISE -> Role.RESPONSABLE_ENTREPRISE;
        };
    }

    private RoleSignature roleSignature(TypeSignature t) {
        return switch (t) {
            case STAGIAIRE -> RoleSignature.STAGIAIRE;
            case ENCADRANT_ACADEMIQUE -> RoleSignature.ENCADRANT_ACADEMIQUE;
            case ENCADRANT_PROFESSIONNEL -> RoleSignature.ENCADRANT_PROFESSIONNEL;
            case RESPONSABLE_ENTREPRISE -> RoleSignature.RESPONSABLE_ENTREPRISE;
        };
    }

    private void requireSavedSignature(Utilisateur utilisateur) {
        if (utilisateur.getUrlSignature() == null || utilisateur.getUrlSignature().isBlank()) {
            throw new RuntimeException("Veuillez ajouter votre signature à votre profil avant de signer ce document.");
        }
    }

    private enum TypeSignature {
        STAGIAIRE, ENCADRANT_ACADEMIQUE, ENCADRANT_PROFESSIONNEL, RESPONSABLE_ENTREPRISE
    }
}
