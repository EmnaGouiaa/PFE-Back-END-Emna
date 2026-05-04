package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.CahierStageMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.services.CahierStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
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

        initBooleans(entity);

        CahierStage saved = cahierStageRepository.save(entity);
        updateEtatSignature(saved);

        return cahierStageMapper.toDto(cahierStageRepository.save(saved));
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

        initBooleans(entity);

        CahierStage saved = cahierStageRepository.save(entity);
        updateEtatSignature(saved);

        return cahierStageMapper.toDto(cahierStageRepository.save(saved));
    }

    @Override
    public CahierStageDto getById(Long id) {
        CahierStage entity = cahierStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable avec l'id : " + id));

        return cahierStageMapper.toDto(entity);
    }

    @Override
    public CahierStageDto getByStageId(Long stageId) {
        CahierStage entity = cahierStageRepository.findByStageId(stageId)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable pour le stage id : " + stageId));

        return cahierStageMapper.toDto(entity);
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

        CahierStage updated = cahierStageRepository.save(entity);
        updateEtatSignature(updated);

        return cahierStageMapper.toDto(cahierStageRepository.save(updated));
    }

    @Override
    public CahierStageDto signerParStagiaire(Long id) {
        return signer(id, TypeSignature.STAGIAIRE);
    }

    @Override
    public CahierStageDto signerParEncadrantAcademique(Long id) {
        return signer(id, TypeSignature.ENCADRANT_ACADEMIQUE);
    }

    @Override
    public CahierStageDto signerParEncadrantProfessionnel(Long id) {
        return signer(id, TypeSignature.ENCADRANT_PROFESSIONNEL);
    }

    @Override
    public CahierStageDto signerParResponsableEntreprise(Long id) {
        return signer(id, TypeSignature.RESPONSABLE_ENTREPRISE);
    }

    @Override
    public void delete(Long id) {
        CahierStage entity = cahierStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable avec l'id : " + id));

        cahierStageRepository.delete(entity);
    }

    private CahierStageDto signer(Long id, TypeSignature typeSignature) {
        CahierStage cahier = getCahierEntity(id);
        Utilisateur utilisateur = getAuthenticatedSigner(typeSignature);
        ensureSignerBelongsToStage(cahier.getStage(), utilisateur, typeSignature);
        LocalDateTime signedAt = LocalDateTime.now();
        String signatureImage = requireSavedSignature(utilisateur);
        String signerName = buildFullName(utilisateur);
        String signerRole = utilisateur.getRole().name();

        switch (typeSignature) {
            case STAGIAIRE -> {
                if (Boolean.TRUE.equals(cahier.getSigneeStagiaire())) {
                    throw new RuntimeException("Le cahier est deja signe par le stagiaire.");
                }
                cahier.setSigneeStagiaire(true);
                cahier.setSignataireStagiaireId(utilisateur.getId());
                cahier.setRoleSignatureStagiaire(signerRole);
                cahier.setNomSignataireStagiaire(signerName);
                cahier.setImageSignatureStagiaire(signatureImage);
                cahier.setDateSignatureStagiaire(signedAt);
            }
            case ENCADRANT_ACADEMIQUE -> {
                if (Boolean.TRUE.equals(cahier.getSigneeEncAcad())) {
                    throw new RuntimeException("Le cahier est deja signe par l'encadrant academique.");
                }
                cahier.setSigneeEncAcad(true);
                cahier.setSignataireEncAcadId(utilisateur.getId());
                cahier.setRoleSignatureEncAcad(signerRole);
                cahier.setNomSignataireEncAcad(signerName);
                cahier.setImageSignatureEncAcad(signatureImage);
                cahier.setDateSignatureEncAcad(signedAt);
            }
            case ENCADRANT_PROFESSIONNEL -> {
                if (Boolean.TRUE.equals(cahier.getSigneeEncPro())) {
                    throw new RuntimeException("Le cahier est deja signe par l'encadrant professionnel.");
                }
                cahier.setSigneeEncPro(true);
                cahier.setSignataireEncProId(utilisateur.getId());
                cahier.setRoleSignatureEncPro(signerRole);
                cahier.setNomSignataireEncPro(signerName);
                cahier.setImageSignatureEncPro(signatureImage);
                cahier.setDateSignatureEncPro(signedAt);
            }
            case RESPONSABLE_ENTREPRISE -> {
                if (Boolean.TRUE.equals(cahier.getSigneeRespEntreprise())) {
                    throw new RuntimeException("Le cahier est deja signe par le representant entreprise.");
                }
                cahier.setSigneeRespEntreprise(true);
                cahier.setSignataireRespEntrepriseId(utilisateur.getId());
                cahier.setRoleSignatureRespEntreprise(signerRole);
                cahier.setNomSignataireRespEntreprise(signerName);
                cahier.setImageSignatureRespEntreprise(signatureImage);
                cahier.setDateSignatureRespEntreprise(signedAt);
            }
        }

        cahier.setDateSignature(LocalDate.now());
        updateEtatSignature(cahier);

        CahierStage saved = cahierStageRepository.save(cahier);
        return cahierStageMapper.toDto(saved);
    }

    private CahierStage getCahierEntity(Long id) {
        return cahierStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cahier introuvable avec l'id : " + id));
    }

    private void initBooleans(CahierStage entity) {
        if (entity.getEstSigne() == null) entity.setEstSigne(false);
        if (entity.getSigneeEncAcad() == null) entity.setSigneeEncAcad(false);
        if (entity.getSigneeEncPro() == null) entity.setSigneeEncPro(false);
        if (entity.getSigneeRespEntreprise() == null) entity.setSigneeRespEntreprise(false);
        if (entity.getSigneeStagiaire() == null) entity.setSigneeStagiaire(false);
    }

    private void updateEtatSignature(CahierStage entity) {
        boolean allSigned =
                Boolean.TRUE.equals(entity.getSigneeEncAcad()) &&
                        Boolean.TRUE.equals(entity.getSigneeEncPro()) &&
                        Boolean.TRUE.equals(entity.getSigneeRespEntreprise()) &&
                        Boolean.TRUE.equals(entity.getSigneeStagiaire());

        entity.setEstSigne(allSigned);
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
        if (stage == null) {
            throw new RuntimeException("Aucun stage n'est associe a ce cahier.");
        }

        boolean authorized = switch (typeSignature) {
            case STAGIAIRE -> stage.getStagiaire() != null && stage.getStagiaire().getId().equals(utilisateur.getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId().equals(utilisateur.getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
            case RESPONSABLE_ENTREPRISE -> stage.getTuteurEntreprise() != null && stage.getTuteurEntreprise().getId().equals(utilisateur.getId());
        };

        if (!authorized) {
            throw new RuntimeException("Action non autorisee : vous n'etes pas associe a ce document.");
        }
    }

    private Role expectedRole(TypeSignature typeSignature) {
        return switch (typeSignature) {
            case STAGIAIRE -> Role.STAGIAIRE;
            case ENCADRANT_ACADEMIQUE -> Role.ENCADRANT_ACADEMIQUE;
            case ENCADRANT_PROFESSIONNEL -> Role.ENCADRANT_PROFESSIONNEL;
            case RESPONSABLE_ENTREPRISE -> Role.RESPONSABLE_ENTREPRISE;
        };
    }

    private String requireSavedSignature(Utilisateur utilisateur) {
        if (utilisateur.getNomFichierSignature() == null || utilisateur.getNomFichierSignature().isBlank()) {
            throw new RuntimeException("Please add your signature in your profile before signing this document.");
        }

        return utilisateur.getNomFichierSignature().trim();
    }

    private String buildFullName(Utilisateur utilisateur) {
        String fullName = ((utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom().trim()) + " "
                + (utilisateur.getNom() == null ? "" : utilisateur.getNom().trim())).trim();
        return fullName.isBlank() ? "Utilisateur" : fullName;
    }

    private enum TypeSignature {
        STAGIAIRE,
        ENCADRANT_ACADEMIQUE,
        ENCADRANT_PROFESSIONNEL,
        RESPONSABLE_ENTREPRISE
    }
}
