package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ConventionStageMapper;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.services.ConventionStageService;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
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

        initBooleans(entity);

        ConventionStage saved = conventionStageRepository.save(entity);
        updateStatutSignatures(saved);

        return conventionStageMapper.toDto(conventionStageRepository.save(saved));
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

        if (entity.getNumConv() == null) {
            entity.setNumConv(generateNumConv());
        }

        if (entity.getDateDebut() == null) {
            entity.setDateDebut(stage.getDateDebut());
        }

        if (entity.getDateFin() == null) {
            entity.setDateFin(stage.getDateFin());
        }

        initBooleans(entity);

        ConventionStage saved = conventionStageRepository.save(entity);
        updateStatutSignatures(saved);

        return conventionStageMapper.toDto(conventionStageRepository.save(saved));
    }

    @Override
    public ConventionStageDto getById(Long id) {
        ConventionStage entity = conventionStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convention introuvable avec l'id : " + id));

        return conventionStageMapper.toDto(entity);
    }

    @Override
    public ConventionStageDto getByStageId(Long stageId) {
        ConventionStage entity = conventionStageRepository.findByStageId(stageId)
                .orElseThrow(() -> new RuntimeException("Convention introuvable pour le stage id : " + stageId));

        return conventionStageMapper.toDto(entity);
    }

    @Override
    public List<ConventionStageDto> getAll() {
        return conventionStageRepository.findAll()
                .stream()
                .map(conventionStageMapper::toDto)
                .toList();
    }

    @Override
    public ConventionStageDto update(Long id, ConventionStageDto dto) {
        ConventionStage entity = conventionStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convention introuvable avec l'id : " + id));

        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
        entity.setDateSignatureResponsableUniversitaire(dto.getDateSignatureResponsableUniversitaire());
        entity.setNomResponsableUniversitaireSignataire(dto.getNomResponsableUniversitaireSignataire());

        ConventionStage updated = conventionStageRepository.save(entity);
        updateStatutSignatures(updated);

        return conventionStageMapper.toDto(conventionStageRepository.save(updated));
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
                .orElseThrow(() -> new RuntimeException("Convention introuvable avec l'id : " + id));

        conventionStageRepository.delete(convention);
    }

    private ConventionStageDto signer(Long id, TypeSignature typeSignature) {
        ConventionStage convention = getConventionEntity(id);
        Utilisateur utilisateur = getAuthenticatedSigner(typeSignature);
        Stage stage = convention.getStage();
        ensureSignerBelongsToStage(stage, utilisateur, typeSignature);
        LocalDateTime signedAt = LocalDateTime.now();
        String signatureImage = requireSavedSignature(utilisateur);
        String signerName = buildFullName(utilisateur);
        String signerRole = utilisateur.getRole().name();

        switch (typeSignature) {
            case STAGIAIRE -> {
                if (Boolean.TRUE.equals(convention.getSigneeStagiaire())) {
                    throw new RuntimeException("La convention est deja signee par le stagiaire.");
                }
                convention.setSigneeStagiaire(true);
                convention.setSignataireStagiaireId(utilisateur.getId());
                convention.setRoleSignatureStagiaire(signerRole);
                convention.setNomSignataireStagiaire(signerName);
                convention.setImageSignatureStagiaire(signatureImage);
                convention.setDateSignatureStagiaire(signedAt);
            }
            case ENCADRANT_ACADEMIQUE -> {
                if (Boolean.TRUE.equals(convention.getSigneeEncAca())) {
                    throw new RuntimeException("La convention est deja signee par l'encadrant academique.");
                }
                convention.setSigneeEncAca(true);
                convention.setSignataireEncAcaId(utilisateur.getId());
                convention.setRoleSignatureEncAca(signerRole);
                convention.setNomSignataireEncAca(signerName);
                convention.setImageSignatureEncAca(signatureImage);
                convention.setDateSignatureEncAca(signedAt);
            }
            case ENCADRANT_PROFESSIONNEL -> {
                if (Boolean.TRUE.equals(convention.getSigneeEncPro())) {
                    throw new RuntimeException("La convention est deja signee par l'encadrant professionnel.");
                }
                convention.setSigneeEncPro(true);
                convention.setSignataireEncProId(utilisateur.getId());
                convention.setRoleSignatureEncPro(signerRole);
                convention.setNomSignataireEncPro(signerName);
                convention.setImageSignatureEncPro(signatureImage);
                convention.setDateSignatureEncPro(signedAt);
            }
            case ENTREPRISE -> {
                if (Boolean.TRUE.equals(convention.getSigneeEntreprise())) {
                    throw new RuntimeException("La convention est deja signee par le representant entreprise.");
                }
                convention.setSigneeEntreprise(true);
                convention.setSignataireEntrepriseId(utilisateur.getId());
                convention.setRoleSignatureEntreprise(signerRole);
                convention.setNomSignataireEntreprise(signerName);
                convention.setImageSignatureEntreprise(signatureImage);
                convention.setDateSignatureEntreprise(signedAt);
            }
            case RESPONSABLE -> {
                if (Boolean.TRUE.equals(convention.getSigneeResp())) {
                    throw new RuntimeException("La convention est deja signee par le responsable universitaire.");
                }

                convention.setSigneeResp(true);
                convention.setSignataireResponsableUniversitaireId(utilisateur.getId());
                convention.setRoleSignatureResponsableUniversitaire(signerRole);
                convention.setImageSignatureResponsableUniversitaire(signatureImage);
                convention.setDateSignatureResponsableUniversitaire(signedAt);
                convention.setNomResponsableUniversitaireSignataire(signerName);
            }
        }

        updateStatutSignatures(convention);
        ConventionStage saved = conventionStageRepository.save(convention);
        return conventionStageMapper.toDto(saved);
    }

    private ConventionStage getConventionEntity(Long id) {
        return conventionStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convention introuvable avec l'id : " + id));
    }

    private void initBooleans(ConventionStage entity) {
        if (entity.getSigneeEncAca() == null) entity.setSigneeEncAca(false);
        if (entity.getSigneeEncPro() == null) entity.setSigneeEncPro(false);
        if (entity.getSigneeEntreprise() == null) entity.setSigneeEntreprise(false);
        if (entity.getSigneeResp() == null) entity.setSigneeResp(false);
        if (entity.getSigneeStagiaire() == null) entity.setSigneeStagiaire(false);
        if (entity.getStatutSignatures() == null) entity.setStatutSignatures(false);
    }

    private Utilisateur getAuthenticatedSigner(TypeSignature typeSignature) {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new RuntimeException("Utilisateur authentifie introuvable."));

        Role expectedRole = expectedRole(typeSignature);
        if (utilisateur.getRole() != expectedRole) {
            throw new RuntimeException("Action non autorisee : votre role ne correspond pas a cette signature.");
        }

        return utilisateur;
    }

    private void ensureSignerBelongsToStage(Stage stage, Utilisateur utilisateur, TypeSignature typeSignature) {
        if (stage == null) {
            throw new RuntimeException("Aucun stage n'est associe a cette convention.");
        }

        boolean authorized = switch (typeSignature) {
            case STAGIAIRE -> stage.getStagiaire() != null && stage.getStagiaire().getId().equals(utilisateur.getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId().equals(utilisateur.getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId().equals(utilisateur.getId());
            case ENTREPRISE -> stage.getTuteurEntreprise() != null && stage.getTuteurEntreprise().getId().equals(utilisateur.getId());
            case RESPONSABLE -> true;
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
            case ENTREPRISE -> Role.RESPONSABLE_ENTREPRISE;
            case RESPONSABLE -> Role.RESPONSABLE_UNIVERSITAIRE_STAGES;
        };
    }

    private String requireSavedSignature(Utilisateur utilisateur) {
        if (utilisateur.getNomFichierSignature() == null || utilisateur.getNomFichierSignature().isBlank()) {
            throw new RuntimeException("Please add your signature in your profile before signing this document.");
        }

        return utilisateur.getNomFichierSignature().trim();
    }

    private String buildFullName(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return "Responsable universitaire des stages";
        }

        String fullName = ((utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom().trim()) + " "
                + (utilisateur.getNom() == null ? "" : utilisateur.getNom().trim())).trim();
        return fullName.isBlank() ? "Responsable universitaire des stages" : fullName;
    }

    private void updateStatutSignatures(ConventionStage entity) {
        boolean allSigned =
                Boolean.TRUE.equals(entity.getSigneeEncAca()) &&
                        Boolean.TRUE.equals(entity.getSigneeEncPro()) &&
                        Boolean.TRUE.equals(entity.getSigneeEntreprise()) &&
                        Boolean.TRUE.equals(entity.getSigneeResp()) &&
                        Boolean.TRUE.equals(entity.getSigneeStagiaire());

        entity.setStatutSignatures(allSigned);
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
        STAGIAIRE,
        ENCADRANT_ACADEMIQUE,
        ENCADRANT_PROFESSIONNEL,
        ENTREPRISE,
        RESPONSABLE
    }
}
