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

/**
 * Implémentation Spring de {@link ConventionStageMapper}.
 * <p>
 * Conversion enrichie {@link ConventionStage} ↔ {@link ConventionStageDto}, utilisée par
 * {@link fsegs.pfebackendemnagouuiaa.services.ConventionStageServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class ConventionStageMapperImpl implements ConventionStageMapper {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * {@inheritDoc}
     * <p>
     * Calcule les indicateurs de signature et enrichit le responsable universitaire signataire si présent.
     */
    @Override
    public ConventionStageDto toDto(ConventionStage entity) {
        if (entity == null) return null;

        ConventionStageDto dto = new ConventionStageDto();
        dto.setId(entity.getId());
        dto.setNumConv(entity.getNumConv());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());

        // Rétrocompatibilité : booléens dérivés de la collection signatures
        dto.setSigneeEncAca(entity.estSignePar(RoleSignature.ENCADRANT_ACADEMIQUE));
        dto.setSigneeEncPro(entity.estSignePar(RoleSignature.ENCADRANT_PROFESSIONNEL));
        dto.setSigneeEntreprise(entity.estSignePar(RoleSignature.RESPONSABLE_ENTREPRISE));
        dto.setSigneeResp(entity.estSignePar(RoleSignature.RESPONSABLE_UNIVERSITAIRE));
        dto.setSigneeStagiaire(entity.estSignePar(RoleSignature.STAGIAIRE));
        dto.setStatutSignatures(entity.estCompletementSigne());

        // Champs dédiés au signataire « responsable universitaire » (lecture seule)
        entity.getSignaturePour(RoleSignature.RESPONSABLE_UNIVERSITAIRE).ifPresent(sig -> {
            dto.setDateSignatureResponsableUniversitaire(sig.getDateSignature());
            if (sig.getSignataireId() != null) {
                utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u ->
                        dto.setNomResponsableUniversitaireSignataire(buildFullName(u)));
            }
        });

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

    /** {@inheritDoc} — relations stage / demande / signatures gérées par le service. */
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

    /** {@link #mapSignatures(List)} — voir {@link CahierStageMapperImpl}. */
    private List<SignatureDto> mapSignatures(List<Signature> signatures) {
        if (signatures == null) return List.of();
        return signatures.stream().map(this::toSignatureDto).toList();
    }

    /**
     * Signature vers DTO ; URL et nom chargés depuis {@link UtilisateurRepository}.
     */
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

    /** Nom complet affiché avec repli « Utilisateur » si vide. */
    private String buildFullName(Utilisateur u) {
        String full = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " "
                + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return full.isBlank() ? "Utilisateur" : full;
    }
}
