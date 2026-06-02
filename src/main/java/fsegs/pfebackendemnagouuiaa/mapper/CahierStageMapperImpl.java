package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.dto.SignatureDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implémentation Spring de {@link CahierStageMapper}.
 * <p>
 * Conversion enrichie {@link CahierStage} ↔ {@link CahierStageDto} (signatures, indicateurs calculés),
 * utilisée par {@link fsegs.pfebackendemnagouuiaa.services.CahierStageServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class CahierStageMapperImpl implements CahierStageMapper {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * {@inheritDoc}
     * <p>
     * Les drapeaux booléens historiques sont dérivés de la collection {@code signatures}, pas stockés en colonnes.
     */
    @Override
    public CahierStageDto toDto(CahierStage entity) {
        if (entity == null) return null;

        CahierStageDto dto = new CahierStageDto();
        dto.setId(entity.getId());
        dto.setDateGeneration(entity.getDateGeneration());
        dto.setDateSignature(entity.getDateSignature());

        // Rétrocompatibilité API : indicateurs calculés par rôle de signature
        dto.setSigneeEncAcad(entity.estSignePar(RoleSignature.ENCADRANT_ACADEMIQUE));
        dto.setSigneeEncPro(entity.estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL));
        dto.setSigneeRespEntreprise(entity.estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE));
        dto.setSigneeStagiaire(entity.estSignePar(RoleSignature.STAGIAIRE));
        dto.setEstSigne(entity.estCompletementSigne());

        dto.setSignatures(mapSignatures(entity.getSignatures()));

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        return dto;
    }

    /** {@inheritDoc} — ne persiste pas les signatures via le mapper. */
    @Override
    public CahierStage toEntity(CahierStageDto dto) {
        if (dto == null) return null;

        CahierStage entity = new CahierStage();
        entity.setId(dto.getId());
        entity.setDateGeneration(dto.getDateGeneration());
        entity.setDateSignature(dto.getDateSignature());

        return entity;
    }

    /**
     * Convertit la collection de signatures ; collection {@code null} → liste vide (pas de NPE côté API).
     */
    private List<SignatureDto> mapSignatures(List<Signature> signatures) {
        if (signatures == null) return List.of();
        return signatures.stream().map(this::toSignatureDto).toList();
    }

    /**
     * Mappe une signature unitaire en enrichissant nom et URL depuis l'utilisateur signataire si besoin.
     */
    private SignatureDto toSignatureDto(Signature sig) {
        SignatureDto dto = new SignatureDto();
        dto.setId(sig.getId());
        dto.setRoleSignature(sig.getRoleSignature());
        dto.setSignataireId(sig.getSignataireId());
        dto.setDateSignature(sig.getDateSignature());

        // Priorité à l'image capturée à l'apposition (preuve visuelle) ; repli sur le profil utilisateur
        if (sig.getUrlSignature() != null && !sig.getUrlSignature().isBlank()) {
            dto.setUrlSignature(sig.getUrlSignature());
        }

        if (sig.getSignataireId() != null) {
            utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u -> {
                dto.setNomSignataire(buildFullName(u));
                if (dto.getUrlSignature() == null || dto.getUrlSignature().isBlank()) {
                    dto.setUrlSignature(u.getUrlSignature());
                }
            });
        }

        return dto;
    }

    /**
     * Construit le nom affiché ; valeur de repli si prénom et nom sont vides après trim.
     */
    private String buildFullName(Utilisateur u) {
        String full = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " "
                + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return full.isBlank() ? "Utilisateur" : full;
    }
}
