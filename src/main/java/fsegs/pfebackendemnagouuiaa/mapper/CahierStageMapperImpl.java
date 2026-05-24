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

@Component
@RequiredArgsConstructor
public class CahierStageMapperImpl implements CahierStageMapper {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public CahierStageDto toDto(CahierStage entity) {
        if (entity == null) return null;

        CahierStageDto dto = new CahierStageDto();
        dto.setId(entity.getId());
        dto.setDateGeneration(entity.getDateGeneration());
        dto.setDateSignature(entity.getDateSignature());

        // Backward-compat boolean flags — computed from signatures collection
        dto.setSigneeEncAcad(entity.estSignePar(RoleSignature.ENCADRANT_ACADEMIQUE));
        dto.setSigneeEncPro(entity.estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL));
        dto.setSigneeRespEntreprise(entity.estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE));
        dto.setSigneeStagiaire(entity.estSignePar(RoleSignature.STAGIAIRE));
        dto.setEstSigne(entity.estCompletementSigne());

        // Full signatures list
        dto.setSignatures(mapSignatures(entity.getSignatures()));

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        return dto;
    }

    @Override
    public CahierStage toEntity(CahierStageDto dto) {
        if (dto == null) return null;

        CahierStage entity = new CahierStage();
        entity.setId(dto.getId());
        entity.setDateGeneration(dto.getDateGeneration());
        entity.setDateSignature(dto.getDateSignature());

        return entity;
    }

    private List<SignatureDto> mapSignatures(List<Signature> signatures) {
        if (signatures == null) return List.of();
        return signatures.stream().map(this::toSignatureDto).toList();
    }

    private SignatureDto toSignatureDto(Signature sig) {
        SignatureDto dto = new SignatureDto();
        dto.setId(sig.getId());
        dto.setRoleSignature(sig.getRoleSignature());
        dto.setSignataireId(sig.getSignataireId());
        dto.setDateSignature(sig.getDateSignature());

        // Image de signature : priorite a celle capturee au moment de l'apposition (nouvelle
        // regle metier — preuve visuelle obligatoire). Si absente (signatures historiques),
        // on retombe sur l'image de profil du signataire pour ne pas casser l'affichage.
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

    private String buildFullName(Utilisateur u) {
        String full = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " "
                + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return full.isBlank() ? "Utilisateur" : full;
    }
}
