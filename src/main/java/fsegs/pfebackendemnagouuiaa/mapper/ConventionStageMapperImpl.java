package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.dto.SignatureDto;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConventionStageMapperImpl implements ConventionStageMapper {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public ConventionStageDto toDto(ConventionStage entity) {
        if (entity == null) return null;

        ConventionStageDto dto = new ConventionStageDto();
        dto.setId(entity.getId());
        dto.setNumConv(entity.getNumConv());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());

        // Backward-compat boolean flags — computed from signatures collection
        dto.setSigneeEncAca(entity.estSignePar(RoleSignature.ENCADRANT_ACADEMIQUE));
        dto.setSigneeEncPro(entity.estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL));
        dto.setSigneeEntreprise(entity.estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE));
        dto.setSigneeResp(entity.estSignePar(RoleSignature.RESPONSABLE_UNIVERSITAIRE));
        dto.setSigneeStagiaire(entity.estSignePar(RoleSignature.STAGIAIRE));
        dto.setStatutSignatures(entity.estCompletementSigne());

        // Enriched responsable universitaire fields
        entity.getSignaturePour(RoleSignature.RESPONSABLE_UNIVERSITAIRE).ifPresent(sig -> {
            dto.setDateSignatureResponsableUniversitaire(sig.getDateSignature());
            if (sig.getSignataireId() != null) {
                utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u ->
                        dto.setNomResponsableUniversitaireSignataire(buildFullName(u)));
            }
        });

        // Full signatures list
        dto.setSignatures(mapSignatures(entity.getSignatures()));

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        if (entity.getDemandeStage() != null) {
            dto.setDemandeStageId(entity.getDemandeStage().getId());
        }

        return dto;
    }

    @Override
    public ConventionStage toEntity(ConventionStageDto dto) {
        if (dto == null) return null;

        ConventionStage entity = new ConventionStage();
        entity.setId(dto.getId());
        entity.setNumConv(dto.getNumConv());
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());

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

        if (sig.getSignataireId() != null) {
            utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u -> {
                dto.setNomSignataire(buildFullName(u));
                dto.setUrlSignature(u.getUrlSignature());
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
